package com.nexushr.notification.service;

import com.nexushr.notification.dto.NotificationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "true")
public class SmtpNotificationEmailSender implements NotificationEmailSender {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpNotificationEmailSender(
        JavaMailSender mailSender,
        @Value("${app.email.from:noreply@nexushr.local}") String from
    ) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(NotificationResponse notification) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(notification.recipientEmail());
        message.setSubject(notification.title());
        message.setText(notification.message());
        mailSender.send(message);
    }
}
