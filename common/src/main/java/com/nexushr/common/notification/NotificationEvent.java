package com.nexushr.common.notification;

public record NotificationEvent(
    String recipientEmail,
    String title,
    String message,
    String type,
    String relatedEntityType,
    String relatedEntityId
) {}
