package com.nexushr.attendance.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "leave_types")
public class LeaveType {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "annual_allowance", nullable = false)
    private int annualAllowance;

    @Column(name = "paid", nullable = false)
    private boolean paid;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LeaveType() {}

    public LeaveType(String code, String name, int annualAllowance, boolean paid) {
        this.id = UUID.randomUUID();
        this.code = code;
        this.name = name;
        this.annualAllowance = annualAllowance;
        this.paid = paid;
        this.active = true;
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getAnnualAllowance() { return annualAllowance; }
    public boolean isPaid() { return paid; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }

    public void update(String code, String name, int annualAllowance,
                       boolean paid, boolean active) {
        this.code = code;
        this.name = name;
        this.annualAllowance = annualAllowance;
        this.paid = paid;
        this.active = active;
    }
}
