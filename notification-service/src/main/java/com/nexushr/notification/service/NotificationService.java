package com.nexushr.notification.service;

import com.nexushr.notification.dto.CreateNotificationRequest;
import com.nexushr.notification.dto.DeliveryFailureResponse;
import com.nexushr.notification.dto.NotificationResponse;
import com.nexushr.notification.entity.Notification;
import com.nexushr.notification.event.NotificationPushEvent;
import com.nexushr.notification.repository.NotificationRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final boolean emailEnabled;
    private final String internalToken;

    public NotificationService(
        NotificationRepository repository,
        JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher eventPublisher,
        @Value("${app.email.enabled:false}") boolean emailEnabled,
        @Value("${app.notifications.internal-token:}") String internalToken
    ) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.eventPublisher = eventPublisher;
        this.emailEnabled = emailEnabled;
        this.internalToken = internalToken;
    }

    @Transactional
    public NotificationResponse create(CreateNotificationRequest request) {
        Notification notification = new Notification();
        populate(notification, request);
        return persist(notification, null);
    }

    @Transactional
    public NotificationResponse createInternal(
        CreateNotificationRequest request, String presentedToken, UUID sourceEventId
    ) {
        if (internalToken.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "Internal notification delivery is not configured"
            );
        }
        if (presentedToken == null || !MessageDigest.isEqual(
            internalToken.getBytes(StandardCharsets.UTF_8),
            presentedToken.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Internal notification token is invalid"
            );
        }
        Notification notification = new Notification();
        notification.setSourceEventId(sourceEventId);
        populate(notification, request);
        int inserted = repository.insertInternalIfAbsent(
            UUID.randomUUID(), sourceEventId, notification.getRecipientEmail(),
            notification.getTitle(), notification.getMessage(), notification.getType(),
            notification.getRelatedEntityType(), notification.getRelatedEntityId()
        );
        Notification saved = repository.findBySourceEventId(sourceEventId)
            .orElseThrow(() -> new IllegalStateException(
                "Internal notification was not available after the idempotent insert"
            ));
        if (inserted > 0) enqueueEmail(saved);
        NotificationResponse response = NotificationResponse.from(saved);
        if (inserted > 0) eventPublisher.publishEvent(new NotificationPushEvent(response));
        return response;
    }

    public List<NotificationResponse> getMyNotifications(String email) {
        return repository.findByRecipientEmailOrderByCreatedAtDesc(email)
            .stream()
            .map(NotificationResponse::from)
            .toList();
    }

    public long getUnreadCount(String email) {
        return repository.countByRecipientEmailAndReadFalse(email);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID id, String email) {
        Notification notification = repository.findByIdAndRecipientEmail(id, email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Notification not found"
            ));

        notification.markAsRead();
        return NotificationResponse.from(notification);
    }

    @Transactional
    public int markAllAsRead(String email) {
        List<Notification> notifications =
            repository.findByRecipientEmailOrderByCreatedAtDesc(email);

        int updated = 0;
        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                notification.markAsRead();
                updated++;
            }
        }
        return updated;
    }

    public List<DeliveryFailureResponse> getDeliveryFailures() {
        return jdbcTemplate.query("""
            SELECT id, notification_id, channel, attempt_count, last_error, last_attempt_at
            FROM notification_delivery_outbox
            WHERE status = 'DEAD'
            ORDER BY created_at DESC
            LIMIT 100
            """, (rs, row) -> new DeliveryFailureResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("notification_id", UUID.class),
                rs.getString("channel"),
                rs.getInt("attempt_count"),
                rs.getString("last_error"),
                rs.getTimestamp("last_attempt_at").toInstant()
            ));
    }

    @Transactional
    public void retryDeliveryFailure(UUID id) {
        int updated = jdbcTemplate.update("""
            UPDATE notification_delivery_outbox
            SET status = 'PENDING', attempt_count = 0,
                next_attempt_at = CURRENT_TIMESTAMP, lease_expires_at = NULL,
                delivered_at = NULL, last_error = NULL
            WHERE id = ? AND status = 'DEAD'
            """, id);
        if (updated == 0) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Failed notification delivery not found"
            );
        }
    }

    private NotificationResponse persist(Notification notification, UUID sourceEventId) {
        notification.setSourceEventId(sourceEventId);
        Notification saved = repository.saveAndFlush(notification);
        enqueueEmail(saved);
        NotificationResponse response = NotificationResponse.from(saved);
        eventPublisher.publishEvent(new NotificationPushEvent(response));
        return response;
    }

    private void enqueueEmail(Notification notification) {
        if (!emailEnabled) return;
        jdbcTemplate.update("""
            INSERT INTO notification_delivery_outbox (
                id, notification_id, channel
            ) VALUES (?, ?, 'EMAIL')
            ON CONFLICT (notification_id, channel) DO NOTHING
            """, UUID.randomUUID(), notification.getId());
    }

    private static void populate(Notification notification, CreateNotificationRequest request) {
        notification.setRecipientEmail(request.recipientEmail().trim().toLowerCase());
        notification.setTitle(request.title().trim());
        notification.setMessage(request.message().trim());
        notification.setType(request.type().trim().toUpperCase());
        notification.setRelatedEntityType(request.relatedEntityType());
        notification.setRelatedEntityId(request.relatedEntityId());
    }
}
