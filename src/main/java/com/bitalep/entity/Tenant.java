package com.bitalep.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tenants")
public class Tenant extends SoftDeletableEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
