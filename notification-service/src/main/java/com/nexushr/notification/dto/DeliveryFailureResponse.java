package com.nexushr.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record DeliveryFailureResponse(
    UUID id,
    UUID notificationId,
    String channel,
    int attempts,
    String lastError,
    Instant failedAt
) {}
