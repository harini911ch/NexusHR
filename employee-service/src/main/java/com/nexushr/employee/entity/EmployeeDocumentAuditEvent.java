package com.nexushr.employee.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "employee_document_audit_events",
    indexes = {
        @Index(name = "idx_employee_document_audit_employee", columnList = "employee_id, occurred_at")
    }
)
public class EmployeeDocumentAuditEvent {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "actor_email", nullable = false, length = 180)
    private String actorEmail;

    @Column(name = "action", nullable = false, length = 30)
    private String action;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected EmployeeDocumentAuditEvent() {}

    public EmployeeDocumentAuditEvent(
        UUID documentId, UUID employeeId, String actorEmail, String action
    ) {
        this.documentId = documentId;
        this.employeeId = employeeId;
        this.actorEmail = actorEmail;
        this.action = action;
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (occurredAt == null) occurredAt = Instant.now();
    }
}
