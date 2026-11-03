package com.nexushr.employee.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "designations")
public class Designation {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Designation() {}

    public Designation(String title, UUID departmentId, String description) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.departmentId = departmentId;
        this.description = description;
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public UUID getDepartmentId() { return departmentId; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }

    public void update(String title, UUID departmentId, String description, boolean active) {
        this.title = title;
        this.departmentId = departmentId;
        this.description = description;
        this.active = active;
    }
}
