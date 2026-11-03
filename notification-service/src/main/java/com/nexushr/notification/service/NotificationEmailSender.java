package com.nexushr.notification.service;

import com.nexushr.notification.dto.NotificationResponse;

public interface NotificationEmailSender {
    void send(NotificationResponse notification);
}
