package com.bitalep.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "application_events")
public class ApplicationEvent extends SoftDeletableEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RequestStatus status;

    @Column(length = 1000)
    private String description;

    @Column(name = "actor_id")
    private UUID actorId;
}
