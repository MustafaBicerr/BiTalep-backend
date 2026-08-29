package com.bitalep.repository;

import com.bitalep.entity.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<AppNotification, UUID>, JpaSpecificationExecutor<AppNotification> {

    Optional<AppNotification> findByIdAndTenantIdAndRecipientIdAndDeletedAtIsNull(UUID id, UUID tenantId, UUID recipientId);

    long countByTenantIdAndRecipientIdAndReadFalseAndDeletedAtIsNull(UUID tenantId, UUID recipientId);

    @Modifying(clearAutomatically = true)
    @Query("update AppNotification n set n.read = true, n.updatedAt = CURRENT_TIMESTAMP where n.tenantId = :tenantId and n.recipientId = :recipientId and n.read = false and n.deletedAt is null")
    int markAllRead(@Param("tenantId") UUID tenantId, @Param("recipientId") UUID recipientId);
}
