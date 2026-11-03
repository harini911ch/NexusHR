package com.nexushr.employee.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "departments")
public class Department {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "manager_email", length = 180)
    private String managerEmail;

    @Column(name = "parent_department_id")
    private UUID parentDepartmentId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Department() {}

    public Department(String name, String description, String managerEmail,
                      UUID parentDepartmentId) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.managerEmail = managerEmail;
        this.parentDepartmentId = parentDepartmentId;
        this.active = true;
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getManagerEmail() { return managerEmail; }
    public UUID getParentDepartmentId() { return parentDepartmentId; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(String name, String description, String managerEmail,
                       UUID parentDepartmentId, boolean active) {
        this.name = name;
        this.description = description;
        this.managerEmail = managerEmail;
        this.parentDepartmentId = parentDepartmentId;
        this.active = active;
    }
}
