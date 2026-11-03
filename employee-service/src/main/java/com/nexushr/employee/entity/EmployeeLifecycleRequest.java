package com.nexushr.employee.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "employee_lifecycle_requests",
    indexes = {
        @Index(name = "idx_employee_lifecycle_employee", columnList = "employee_id"),
        @Index(name = "idx_employee_lifecycle_status", columnList = "status, requested_at")
    }
)
public class EmployeeLifecycleRequest {

    @Id
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private EmployeeLifecycleAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmployeeLifecycleStatus status = EmployeeLifecycleStatus.PENDING;

    @Column(name = "requested_by", nullable = false, length = 180)
    private String requestedBy;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "reviewed_by", length = 180)
    private String reviewedBy;

    @Column(name = "review_note", length = 1000)
    private String reviewNote;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected EmployeeLifecycleRequest() {}

    public EmployeeLifecycleRequest(
        UUID employeeId,
        EmployeeLifecycleAction action,
        String requestedBy,
        String reason
    ) {
        this.id = UUID.randomUUID();
        this.employeeId = employeeId;
        this.action = action;
        this.requestedBy = requestedBy;
        this.reason = reason;
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (requestedAt == null) requestedAt = Instant.now();
    }

    public void decide(
        EmployeeLifecycleStatus decision,
        String reviewerEmail,
        String note,
        Instant now
    ) {
        this.status = decision;
        this.reviewedBy = reviewerEmail;
        this.reviewNote = note;
        this.reviewedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public EmployeeLifecycleAction getAction() { return action; }
    public EmployeeLifecycleStatus getStatus() { return status; }
    public String getRequestedBy() { return requestedBy; }
    public String getReason() { return reason; }
    public String getReviewedBy() { return reviewedBy; }
    public String getReviewNote() { return reviewNote; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
}
