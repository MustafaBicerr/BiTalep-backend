package com.bitalep.repository;

import com.bitalep.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByIdAndDeletedAtIsNull(UUID id);
}
