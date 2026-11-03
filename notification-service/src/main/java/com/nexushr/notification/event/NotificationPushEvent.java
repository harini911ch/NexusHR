package com.nexushr.notification.event;

import com.nexushr.notification.dto.NotificationResponse;

public record NotificationPushEvent(NotificationResponse notification) {}
