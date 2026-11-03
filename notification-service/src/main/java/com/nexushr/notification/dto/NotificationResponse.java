package com.nexushr.notification.dto;

import com.nexushr.notification.entity.Notification;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    String recipientEmail,
    String title,
    String message,
    String type,
    String relatedEntityType,
    String relatedEntityId,
    boolean read,
    Instant createdAt,
    Instant readAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
            n.getId(),
            n.getRecipientEmail(),
            n.getTitle(),
            n.getMessage(),
            n.getType(),
            n.getRelatedEntityType(),
            n.getRelatedEntityId(),
            n.isRead(),
            n.getCreatedAt(),
            n.getReadAt()
        );
    }
}
