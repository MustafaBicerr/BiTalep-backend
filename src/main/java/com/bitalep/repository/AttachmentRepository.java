package com.bitalep.repository;

import com.bitalep.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID>, JpaSpecificationExecutor<Attachment> {

    Optional<Attachment> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    List<Attachment> findByTenantIdAndApplicationIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID tenantId, UUID applicationId);

    long countByTenantIdAndApplicationIdAndDeletedAtIsNull(UUID tenantId, UUID applicationId);
}
