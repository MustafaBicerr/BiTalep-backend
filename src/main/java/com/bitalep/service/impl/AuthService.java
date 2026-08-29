package com.bitalep.service.impl;

import com.bitalep.config.AppProperties;
import com.bitalep.dto.UserDtos;
import com.bitalep.entity.AppUser;
import com.bitalep.entity.Department;
import com.bitalep.entity.RefreshToken;
import com.bitalep.entity.Subscription;
import com.bitalep.entity.Tenant;
import com.bitalep.entity.UserRole;
import com.bitalep.exception.ApiException;
import com.bitalep.mail.MailService;
import com.bitalep.mapper.DtoMapper;
import com.bitalep.repository.PasswordResetTokenRepository;
import com.bitalep.repository.RefreshTokenRepository;
import com.bitalep.repository.SubscriptionRepository;
import com.bitalep.repository.TenantRepository;
import com.bitalep.repository.UserRepository;
import com.bitalep.security.JwtService;
import com.bitalep.security.TenantContext;
import com.bitalep.entity.PasswordResetToken;
import com.bitalep.util.TokenHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final TenantRepository tenants;
    private final SubscriptionRepository subscriptions;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties props;
    private final MailService mailService;

    public AuthService(
            UserRepository users,
            TenantRepository tenants,
            SubscriptionRepository subscriptions,
            RefreshTokenRepository refreshTokens,
            PasswordResetTokenRepository resetTokens,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AppProperties props,
            MailService mailService
    ) {
        this.users = users;
        this.tenants = tenants;
        this.subscriptions = subscriptions;
        this.refreshTokens = refreshTokens;
        this.resetTokens = resetTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.props = props;
        this.mailService = mailService;
    }

    @Transactional
    public UserDtos.LoginResponse login(UserDtos.LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        List<AppUser> matches = users.findByEmailIgnoreCaseAndDeletedAtIsNullAndActiveTrue(email);
        AppUser user = matches.stream()
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .findFirst()
                .orElseThrow(ApiException::unauthorized);
        return issue(user);
    }

    @Transactional
    public UserDtos.LoginResponse register(UserDtos.RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (!users.findByEmailIgnoreCaseAndDeletedAtIsNullAndActiveTrue(email).isEmpty()) {
            throw ApiException.conflict();
        }
        Tenant tenant = new Tenant();
        tenant.setName(req.companyName().trim());
        tenant.setActive(true);
        tenants.save(tenant);

        AppUser admin = new AppUser();
        admin.setTenantId(tenant.getId());
        admin.setName(req.name().trim());
        admin.setSurname(req.surname().trim());
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(req.password()));
        admin.setRole(UserRole.ADMIN);
        admin.setDepartment(Department.OTHER);
        admin.setActive(true);
        admin.setCreatedBy(null);
        users.save(admin);
        admin.setCreatedBy(admin.getId());
        admin.setUpdatedBy(admin.getId());
        users.save(admin);

        Subscription sub = new Subscription();
        sub.setTenantId(tenant.getId());
        sub.setPlan("PRO");
        sub.setActive(true);
        sub.setCurrentPeriodEnd(nextPeriodEnd());
        sub.setCreatedBy(admin.getId());
        sub.setUpdatedBy(admin.getId());
        subscriptions.save(sub);

        mailService.sendWelcome(admin.getEmail(), admin.getName(), admin.getSurname());
        return issue(admin);
    }

    @Transactional
    public UserDtos.TokenPairResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw ApiException.unauthorized();
        }
        String hash = TokenHasher.sha256(refreshToken);
        RefreshToken stored = refreshTokens.findByTokenHash(hash).orElseThrow(ApiException::unauthorized);
        Instant now = Instant.now();
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(now)) {
            if (stored.isRevoked()) {
                refreshTokens.revokeFamily(stored.getFamilyId(), now);
            }
            throw ApiException.unauthorized();
        }
        AppUser user = users.findByIdAndTenantIdAndDeletedAtIsNull(stored.getUserId(), stored.getTenantId())
                .filter(AppUser::isActive)
                .orElseThrow(ApiException::unauthorized);

        stored.setRevokedAt(now);
        RefreshToken next = newRefresh(user, stored.getFamilyId());
        stored.setReplacedBy(next.getId());
        refreshTokens.save(stored);
        refreshTokens.save(next);

        String access = jwtService.createAccessToken(user.getId(), user.getTenantId(), user.getRole());
        return new UserDtos.TokenPairResponse(access, next.getRaw());
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            UUID userId = TenantContext.userIdOrNull();
            if (userId != null) {
                refreshTokens.revokeAllForUser(userId, Instant.now());
            }
            return;
        }
        refreshTokens.findByTokenHash(TokenHasher.sha256(refreshToken)).ifPresent(t -> {
            t.setRevokedAt(Instant.now());
            refreshTokens.save(t);
        });
    }

    @Transactional
    public void forgotPassword(String email) {
        List<AppUser> matches = users.findByEmailIgnoreCaseAndDeletedAtIsNullAndActiveTrue(email.trim().toLowerCase());
        if (matches.isEmpty()) {
            return;
        }
        AppUser user = matches.getFirst();
        PasswordResetToken token = new PasswordResetToken();
        String raw = TokenHasher.randomToken();
        token.setTenantId(user.getTenantId());
        token.setUserId(user.getId());
        token.setTokenHash(TokenHasher.sha256(raw));
        token.setExpiresAt(Instant.now().plusSeconds(30 * 60));
        resetTokens.save(token);
        mailService.sendForgotPassword(user.getEmail(), user.getName(), user.getSurname(), mailService.resetUrl(raw));
    }

    @Transactional
    public void resetPassword(String rawToken, String password) {
        PasswordResetToken token = resetTokens.findByTokenHash(TokenHasher.sha256(rawToken))
                .orElseThrow(ApiException::unauthorized);
        Instant now = Instant.now();
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(now)) {
            throw ApiException.unauthorized();
        }
        AppUser user = users.findByIdAndTenantIdAndDeletedAtIsNull(token.getUserId(), token.getTenantId())
                .orElseThrow(ApiException::unauthorized);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setUpdatedBy(user.getId());
        users.save(user);
        token.setUsedAt(now);
        resetTokens.save(token);
        refreshTokens.revokeAllForUser(user.getId(), now);
        mailService.sendPasswordChanged(user.getEmail(), user.getName(), user.getSurname());
    }

    private UserDtos.LoginResponse issue(AppUser user) {
        String access = jwtService.createAccessToken(user.getId(), user.getTenantId(), user.getRole());
        RefreshToken refresh = newRefresh(user, UUID.randomUUID());
        refreshTokens.save(refresh);
        return new UserDtos.LoginResponse(access, refresh.getRaw(), DtoMapper.user(user));
    }

    private RefreshToken newRefresh(AppUser user, UUID familyId) {
        String raw = TokenHasher.randomToken();
        RefreshToken t = new RefreshToken();
        t.setTenantId(user.getTenantId());
        t.setUserId(user.getId());
        t.setFamilyId(familyId);
        t.setTokenHash(TokenHasher.sha256(raw));
        t.setExpiresAt(Instant.now().plusSeconds(props.refresh().days() * 86400));
        t.setCreatedBy(user.getId());
        t.setUpdatedBy(user.getId());
        t.setRaw(raw);
        return t;
    }

    static Instant nextPeriodEnd() {
        LocalDate next = LocalDate.now(ZoneOffset.UTC).plusMonths(1);
        return next.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
