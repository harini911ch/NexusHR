package com.nexushr.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "app.auth.password-reset.email-enabled",
    havingValue = "true"
)
public class SmtpPasswordResetEmailSender implements PasswordResetEmailSender {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpPasswordResetEmailSender(
        JavaMailSender mailSender,
        @Value("${spring.mail.from:noreply@nexushr.local}") String from
    ) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(String recipient, String resetUrl, long expiresInMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Reset your NexusHR password");
        message.setText(
            "A password reset was requested for your NexusHR account.\n\n" +
            "Use this one-time link within " + expiresInMinutes + " minutes:\n" +
            resetUrl + "\n\n" +
            "If you did not request this change, you can ignore this email."
        );
        mailSender.send(message);
    }
}
