package com.bitalep.security;

import com.bitalep.config.AppProperties;
import com.bitalep.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessMinutes;

    public JwtService(AppProperties props) {
        byte[] bytes = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessMinutes = props.jwt().accessMinutes();
    }

    public String createAccessToken(UUID userId, UUID tenantId, UserRole role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessMinutes * 60);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public AccessPrincipal parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            UUID tenantId = UUID.fromString(claims.get("tenantId", String.class));
            UserRole role = UserRole.valueOf(claims.get("role", String.class));
            return new AccessPrincipal(userId, tenantId, role);
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    public record AccessPrincipal(UUID userId, UUID tenantId, UserRole role) {}
}
