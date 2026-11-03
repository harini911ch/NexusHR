package com.nexushr.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_audit_events")
public class AuthAuditEvent {

    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "actor_email", length = 180)
    private String actorEmail;

    @Column(name = "subject_email", length = 180)
    private String subjectEmail;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuthAuditEvent() {}

    public AuthAuditEvent(String eventType, String actorEmail, String subjectEmail) {
        this.eventType = eventType;
        this.actorEmail = actorEmail;
        this.subjectEmail = subjectEmail;
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (occurredAt == null) occurredAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getEventType() { return eventType; }
    public String getActorEmail() { return actorEmail; }
    public String getSubjectEmail() { return subjectEmail; }
    public Instant getOccurredAt() { return occurredAt; }
}
