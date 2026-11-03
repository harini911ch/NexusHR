package com.nexushr.auth.service;

public interface PasswordResetEmailSender {
    void send(String recipient, String resetUrl, long expiresInMinutes);
}
