package com.bitalep.service.impl;

import com.bitalep.config.AppProperties;
import com.bitalep.dto.PaginationMeta;
import com.bitalep.dto.UserDtos;
import com.bitalep.entity.AppUser;
import com.bitalep.entity.Department;
import com.bitalep.entity.UserRole;
import com.bitalep.exception.ApiException;
import com.bitalep.mail.MailService;
import com.bitalep.mapper.DtoMapper;
import com.bitalep.repository.UserRepository;
import com.bitalep.security.TenantContext;
import com.bitalep.util.TokenHasher;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AppProperties props;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder, MailService mailService, AppProperties props) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.props = props;
    }

    public UserDtos.UserResponse me() {
        return DtoMapper.user(requireMe());
    }

    @Transactional
    public UserDtos.UserResponse updateMe(UserDtos.UpdateProfileRequest req) {
        AppUser me = requireMe();
        me.setName(req.name().trim());
        me.setSurname(req.surname().trim());
        me.setUpdatedBy(me.getId());
        return DtoMapper.user(users.save(me));
    }

    public UserDtos.UserResponse get(UUID id) {
        requireAdmin();
        return DtoMapper.user(load(id));
    }

    public record UserPage(List<UserDtos.UserResponse> data, PaginationMeta meta) {}

    public UserPage list(int page, int pageSize, String sortBy, String sortOrder, String keyword, UserRole role, Department department) {
        requireAdmin();
        UUID tenantId = TenantContext.tenantId();
        int p = Math.max(page, 1);
        int size = pageSize <= 0 ? 10 : Math.min(pageSize, 100);
        Sort sort = Sort.by(sortDir(sortOrder), mapUserSort(sortBy));
        Specification<AppUser> spec = (root, q, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("tenantId"), tenantId));
            preds.add(cb.isNull(root.get("deletedAt")));
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("surname")), like),
                        cb.like(cb.lower(root.get("email")), like)
                ));
            }
            if (role != null) {
                preds.add(cb.equal(root.get("role"), role));
            }
            if (department != null) {
                preds.add(cb.equal(root.get("department"), department));
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
        Page<AppUser> result = users.findAll(spec, PageRequest.of(p - 1, size, sort));
        return new UserPage(
                result.getContent().stream().map(DtoMapper::user).toList(),
                PaginationMeta.of(p, size, result.getTotalElements())
        );
    }

    @Transactional
    public UserDtos.UserResponse create(UserDtos.CreateUserRequest req, String demoSeedHeader) {
        requireAdmin();
        UUID tenantId = TenantContext.tenantId();
        String email = req.email().trim().toLowerCase();
        if (users.existsByTenantIdAndEmailIgnoreCaseAndDeletedAtIsNull(tenantId, email)) {
            throw ApiException.conflict();
        }
        String password;
        boolean demo = demoSeedAllowed(demoSeedHeader) && req.password() != null && !req.password().isBlank();
        if (demo) {
            password = req.password();
        } else {
            password = TokenHasher.randomPassword();
        }
        AppUser user = new AppUser();
        user.setTenantId(tenantId);
        user.setName(req.name().trim());
        user.setSurname(req.surname().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(req.role() == null ? UserRole.PERSONEL : req.role());
        user.setDepartment(req.department() == null ? Department.OTHER : req.department());
        user.setActive(true);
        user.setCreatedBy(TenantContext.userId());
        user.setUpdatedBy(TenantContext.userId());
        users.save(user);
        mailService.sendInvite(user.getEmail(), user.getName(), user.getSurname(), password);
        return DtoMapper.user(user);
    }

    @Transactional
    public UserDtos.UserResponse updateRole(UUID id, UserRole role) {
        requireAdmin();
        if (role == null) {
            throw ApiException.validation("errors:validation");
        }
        if (id.equals(TenantContext.userId()) && role != UserRole.ADMIN) {
            throw ApiException.conflict();
        }
        AppUser user = load(id);
        user.setRole(role);
        user.setUpdatedBy(TenantContext.userId());
        return DtoMapper.user(users.save(user));
    }

    private boolean demoSeedAllowed(String header) {
        String expected = props.demoSeedKey();
        return expected != null && !expected.isBlank() && expected.equals(header);
    }

    private AppUser requireMe() {
        return users.findByIdAndTenantIdAndDeletedAtIsNull(TenantContext.userId(), TenantContext.tenantId())
                .orElseThrow(ApiException::unauthorized);
    }

    private AppUser load(UUID id) {
        return users.findByIdAndTenantIdAndDeletedAtIsNull(id, TenantContext.tenantId())
                .orElseThrow(ApiException::notFound);
    }

    private static void requireAdmin() {
        if (!TenantContext.isAdmin()) {
            throw ApiException.forbidden();
        }
    }

    private static Sort.Direction sortDir(String order) {
        return "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    private static String mapUserSort(String sortBy) {
        if (sortBy == null) {
            return "createdAt";
        }
        return switch (sortBy) {
            case "name" -> "name";
            case "email" -> "email";
            case "role" -> "role";
            case "createdDate" -> "createdAt";
            default -> "createdAt";
        };
    }
}
