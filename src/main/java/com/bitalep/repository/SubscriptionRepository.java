package com.bitalep.repository;

import com.bitalep.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    List<Subscription> findByActiveTrueAndDeletedAtIsNullAndCurrentPeriodEndBefore(Instant cutoff);
}
