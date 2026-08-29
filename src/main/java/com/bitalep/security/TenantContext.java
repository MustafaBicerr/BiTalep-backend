package com.bitalep.security;

import com.bitalep.entity.UserRole;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> TENANT = new ThreadLocal<>();
    private static final ThreadLocal<UUID> USER = new ThreadLocal<>();
    private static final ThreadLocal<UserRole> ROLE = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID tenantId, UUID userId, UserRole role) {
        TENANT.set(tenantId);
        USER.set(userId);
        ROLE.set(role);
    }

    public static UUID tenantId() {
        UUID id = TENANT.get();
        if (id == null) {
            throw new IllegalStateException("No tenant in context");
        }
        return id;
    }

    public static UUID tenantIdOrNull() {
        return TENANT.get();
    }

    public static UUID userId() {
        UUID id = USER.get();
        if (id == null) {
            throw new IllegalStateException("No user in context");
        }
        return id;
    }

    public static UUID userIdOrNull() {
        return USER.get();
    }

    public static UserRole role() {
        return ROLE.get();
    }

    public static boolean isAdmin() {
        return ROLE.get() == UserRole.ADMIN;
    }

    public static void clear() {
        TENANT.remove();
        USER.remove();
        ROLE.remove();
    }
}
