package com.nexushr.payroll.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payroll_runs", uniqueConstraints = @UniqueConstraint(
    name = "uq_payroll_run_period", columnNames = {"payroll_month", "payroll_year"}
))
public class PayrollRun {

    @Id
    private UUID id;

    @Column(name = "payroll_month", nullable = false)
    private int month;

    @Column(name = "payroll_year", nullable = false)
    private int year;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "created_by", nullable = false, length = 180)
    private String createdBy;

    @Column(name = "approved_by", length = 180)
    private String approvedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    protected PayrollRun() {}

    public PayrollRun(int month, int year, String createdBy) {
        this.id = UUID.randomUUID();
        this.month = month;
        this.year = year;
        this.createdBy = createdBy;
        this.status = "PENDING_APPROVAL";
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public void approve(String reviewer) {
        if (!"PENDING_APPROVAL".equals(status)) {
            throw new IllegalStateException("Only pending payroll runs can be approved");
        }
        status = "APPROVED";
        approvedBy = reviewer;
        approvedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public int getMonth() { return month; }
    public int getYear() { return year; }
    public String getStatus() { return status; }
    public String getCreatedBy() { return createdBy; }
    public String getApprovedBy() { return approvedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getApprovedAt() { return approvedAt; }
}
