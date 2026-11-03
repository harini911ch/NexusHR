package com.nexushr.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNotificationRequest(
    @NotBlank @Email @Size(max = 255) String recipientEmail,
    @NotBlank @Size(max = 150) String title,
    @NotBlank @Size(max = 2000) String message,
    @NotBlank @Size(max = 50) String type,
    @Size(max = 100) String relatedEntityType,
    @Size(max = 100) String relatedEntityId
) {}
