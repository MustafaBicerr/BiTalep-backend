package com.bitalep.repository;

import com.bitalep.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID>, JpaSpecificationExecutor<Application> {

    Optional<Application> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
}
