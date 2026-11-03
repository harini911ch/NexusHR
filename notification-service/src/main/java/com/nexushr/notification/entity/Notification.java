package com.nexushr.notification.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    private UUID id;

    @Column(name = "source_event_id", unique = true)
    private UUID sourceEventId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "related_entity_type", length = 100)
    private String relatedEntityType;

    @Column(name = "related_entity_id", length = 100)
    private String relatedEntityId;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getSourceEventId() { return sourceEventId; }
    public String getRecipientEmail() { return recipientEmail; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public String getRelatedEntityType() { return relatedEntityType; }
    public String getRelatedEntityId() { return relatedEntityId; }
    public boolean isRead() { return read; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }

    public void setRecipientEmail(String value) { recipientEmail = value; }
    public void setSourceEventId(UUID value) { sourceEventId = value; }
    public void setTitle(String value) { title = value; }
    public void setMessage(String value) { message = value; }
    public void setType(String value) { type = value; }
    public void setRelatedEntityType(String value) { relatedEntityType = value; }
    public void setRelatedEntityId(String value) { relatedEntityId = value; }

    public void markAsRead() {
        if (!read) {
            read = true;
            readAt = Instant.now();
        }
    }
}
