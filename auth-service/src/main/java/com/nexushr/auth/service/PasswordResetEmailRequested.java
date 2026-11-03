package com.nexushr.auth.service;

public record PasswordResetEmailRequested(
    String recipient,
    String resetUrl,
    String normalizedEmail,
    long expiresInMinutes
) {}
