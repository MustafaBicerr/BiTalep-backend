package com.bitalep.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "subscriptions")
public class Subscription extends SoftDeletableEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 32)
    private String plan = "PRO";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "current_period_end", nullable = false)
    private Instant currentPeriodEnd;
}
