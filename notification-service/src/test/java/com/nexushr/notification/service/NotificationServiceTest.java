package com.nexushr.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexushr.notification.dto.CreateNotificationRequest;
import com.nexushr.notification.entity.Notification;
import com.nexushr.notification.event.NotificationPushEvent;
import com.nexushr.notification.repository.NotificationRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository repository;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Test
    void manualNotificationIsPersistedAndQueuedForAfterCommitWebSocketPush() {
        NotificationService service =
            new NotificationService(repository, jdbcTemplate, eventPublisher, false, "internal-secret");
        when(repository.saveAndFlush(any(Notification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new CreateNotificationRequest(
            "Employee@Example.com", "Welcome", "Welcome to NexusHR", "general", null, null
        ));

        assertEquals("employee@example.com", response.recipientEmail());
        assertEquals("GENERAL", response.type());
        verify(eventPublisher).publishEvent(any(NotificationPushEvent.class));
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void internalEventRequiresConfiguredToken() {
        NotificationService service =
            new NotificationService(repository, jdbcTemplate, eventPublisher, false, "internal-secret");

        var exception = assertThrows(
            ResponseStatusException.class,
            () -> service.createInternal(new CreateNotificationRequest(
                "employee@example.com", "Title", "Message", "PAYROLL", null, null
            ), "wrong-token", UUID.randomUUID())
        );

        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void internalEventUsesStableIdempotencyKey() {
        NotificationService service =
            new NotificationService(repository, jdbcTemplate, eventPublisher, false, "internal-secret");
        UUID eventId = UUID.randomUUID();
        Notification existing = new Notification();
        existing.setRecipientEmail("employee@example.com");
        existing.setTitle("Leave approved");
        existing.setMessage("Your leave was approved");
        existing.setType("LEAVE");
        when(repository.insertInternalIfAbsent(
            any(), eq(eventId), anyString(), anyString(), anyString(), anyString(), any(), any()
        )).thenReturn(1);
        when(repository.findBySourceEventId(eventId)).thenReturn(Optional.of(existing));

        var response = service.createInternal(new CreateNotificationRequest(
            "employee@example.com", "Leave approved", "Your leave was approved",
            "LEAVE", "LEAVE_REQUEST", "request-123"
        ), "internal-secret", eventId);

        assertEquals("employee@example.com", response.recipientEmail());
        assertEquals("LEAVE", response.type());
        verify(eventPublisher).publishEvent(any(NotificationPushEvent.class));
    }

    @Test
    void duplicateInternalEventDoesNotCreateAnotherPush() {
        NotificationService service =
            new NotificationService(repository, jdbcTemplate, eventPublisher, false, "internal-secret");
        UUID eventId = UUID.randomUUID();
        Notification existing = new Notification();
        existing.setRecipientEmail("employee@example.com");
        existing.setTitle("Leave approved");
        existing.setMessage("Your leave was approved");
        existing.setType("LEAVE");
        when(repository.insertInternalIfAbsent(
            any(), eq(eventId), anyString(), anyString(), anyString(), anyString(), any(), any()
        )).thenReturn(0);
        when(repository.findBySourceEventId(eventId)).thenReturn(Optional.of(existing));

        service.createInternal(new CreateNotificationRequest(
            "employee@example.com", "Leave approved", "Your leave was approved",
            "LEAVE", "LEAVE_REQUEST", "request-123"
        ), "internal-secret", eventId);

        verify(eventPublisher, never()).publishEvent(any(NotificationPushEvent.class));
    }
}
