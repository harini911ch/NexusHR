package com.nexushr.auth.service;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

@ExtendWith(MockitoExtension.class)
class PasswordResetEmailListenerTest {

    @Mock private PasswordResetEmailSender emailSender;
    @Mock private AuthAuditService auditService;

    @Test
    void recordsEmailDeliveryFailureWithoutExposingTheResetToken() {
        PasswordResetEmailListener listener =
            new PasswordResetEmailListener(emailSender, auditService);
        PasswordResetEmailRequested event = new PasswordResetEmailRequested(
            "member@example.com",
            "https://nexushr.example/reset-password#token=secret-token",
            "member@example.com",
            30
        );
        doThrow(new MailSendException("SMTP unavailable"))
            .when(emailSender)
            .send(
                event.recipient(), event.resetUrl(), event.expiresInMinutes()
            );

        listener.sendResetEmail(event);

        verify(auditService).record(
            "PASSWORD_RESET_EMAIL_FAILED", null, "member@example.com"
        );
    }

    @Test
    void recordsSuccessfulEmailDelivery() {
        PasswordResetEmailListener listener =
            new PasswordResetEmailListener(emailSender, auditService);
        PasswordResetEmailRequested event = new PasswordResetEmailRequested(
            "member@example.com",
            "https://nexushr.example/reset-password#token=secret-token",
            "member@example.com",
            30
        );

        listener.sendResetEmail(event);

        verify(auditService).record(
            "PASSWORD_RESET_EMAIL_SENT", null, "member@example.com"
        );
    }
}
