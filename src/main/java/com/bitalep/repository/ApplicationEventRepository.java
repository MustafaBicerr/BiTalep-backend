package com.bitalep.repository;

import com.bitalep.entity.ApplicationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationEventRepository extends JpaRepository<ApplicationEvent, UUID> {

    List<ApplicationEvent> findByTenantIdAndApplicationIdAndDeletedAtIsNullOrderByCreatedAtAsc(
            UUID tenantId, UUID applicationId);
}
