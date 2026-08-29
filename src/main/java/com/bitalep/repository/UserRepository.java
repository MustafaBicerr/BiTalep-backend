package com.bitalep.repository;

import com.bitalep.entity.AppUser;
import com.bitalep.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<AppUser, UUID>, JpaSpecificationExecutor<AppUser> {

    Optional<AppUser> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<AppUser> findByTenantIdAndEmailIgnoreCaseAndDeletedAtIsNull(UUID tenantId, String email);

    List<AppUser> findByEmailIgnoreCaseAndDeletedAtIsNullAndActiveTrue(String email);

    List<AppUser> findByTenantIdAndRoleAndDeletedAtIsNullAndActiveTrue(UUID tenantId, UserRole role);

    boolean existsByTenantIdAndEmailIgnoreCaseAndDeletedAtIsNull(UUID tenantId, String email);
}
