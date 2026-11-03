package com.nexushr.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(
    name = "app.auth.password-reset.email-enabled",
    havingValue = "true"
)
public class PasswordResetEmailListener {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(PasswordResetEmailListener.class);

    private final PasswordResetEmailSender emailSender;
    private final AuthAuditService auditService;

    public PasswordResetEmailListener(
        PasswordResetEmailSender emailSender,
        AuthAuditService auditService
    ) {
        this.emailSender = emailSender;
        this.auditService = auditService;
    }

    @Async("applicationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendResetEmail(PasswordResetEmailRequested event) {
        try {
            emailSender.send(
                event.recipient(), event.resetUrl(), event.expiresInMinutes()
            );
            auditService.record(
                "PASSWORD_RESET_EMAIL_SENT", null, event.normalizedEmail()
            );
        } catch (MailException exception) {
            auditService.record(
                "PASSWORD_RESET_EMAIL_FAILED", null, event.normalizedEmail()
            );
            LOGGER.error("Password reset email delivery failed", exception);
        }
    }
}
