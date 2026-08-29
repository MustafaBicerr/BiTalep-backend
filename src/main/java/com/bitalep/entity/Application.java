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
@Table(name = "applications")
public class Application extends SoftDeletableEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "applicant_id", nullable = false)
    private UUID applicantId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "form_type", nullable = false, length = 32)
    private FormType formType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RequestStatus status = RequestStatus.NEW;

    @Column(name = "reject_reason", length = 1000)
    private String rejectReason;

    @Column(name = "update_reason", length = 1000)
    private String updateReason;
}
