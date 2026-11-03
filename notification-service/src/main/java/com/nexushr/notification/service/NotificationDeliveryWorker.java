package com.nexushr.notification.service;

import com.nexushr.notification.dto.NotificationResponse;
import com.nexushr.notification.repository.NotificationRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.annotation.Value;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "true")
public class NotificationDeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryWorker.class);
    private static final int MAX_ERROR_LENGTH = 1000;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final NotificationRepository notificationRepository;
    private final NotificationEmailSender emailSender;
    private final int batchSize;
    private final int maxAttempts;
    private final int leaseSeconds;

    public NotificationDeliveryWorker(
        JdbcTemplate jdbcTemplate,
        TransactionTemplate transactionTemplate,
        NotificationRepository notificationRepository,
        NotificationEmailSender emailSender,
        @Value("${app.email.delivery.batch-size:20}") int batchSize,
        @Value("${app.email.delivery.max-attempts:12}") int maxAttempts,
        @Value("${app.email.delivery.lease-seconds:60}") int leaseSeconds
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.notificationRepository = notificationRepository;
        this.emailSender = emailSender;
        this.batchSize = Math.max(1, batchSize);
        this.maxAttempts = Math.max(1, maxAttempts);
        this.leaseSeconds = Math.max(5, leaseSeconds);
    }

    @Scheduled(fixedDelayString = "${app.email.delivery.poll-interval-ms:2000}")
    public void processDueDeliveries() {
        List<DeliveryRow> claimed = transactionTemplate.execute(status -> jdbcTemplate.query("""
            WITH due AS (
                SELECT id
                FROM notification_delivery_outbox
                WHERE (status = 'PENDING' AND next_attempt_at <= CURRENT_TIMESTAMP)
                   OR (status = 'PROCESSING' AND lease_expires_at < CURRENT_TIMESTAMP)
                ORDER BY next_attempt_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            UPDATE notification_delivery_outbox AS item
            SET status = 'PROCESSING',
                attempt_count = item.attempt_count + 1,
                last_attempt_at = CURRENT_TIMESTAMP,
                lease_expires_at = CURRENT_TIMESTAMP + (? * INTERVAL '1 second')
            FROM due
            WHERE item.id = due.id
            RETURNING item.id, item.notification_id, item.attempt_count
            """, (rs, row) -> new DeliveryRow(
                rs.getObject("id", UUID.class),
                rs.getObject("notification_id", UUID.class),
                rs.getInt("attempt_count")
            ), batchSize, leaseSeconds));
        if (claimed == null) return;

        for (DeliveryRow row : claimed) {
            deliver(row);
        }
    }

    private void deliver(DeliveryRow row) {
        try {
            NotificationResponse response = notificationRepository.findById(row.notificationId())
                .map(NotificationResponse::from)
                .orElseThrow(() -> new IllegalStateException(
                    "Notification delivery references a missing notification"
                ));
            emailSender.send(response);
            jdbcTemplate.update("""
                UPDATE notification_delivery_outbox
                SET status = 'DELIVERED', delivered_at = CURRENT_TIMESTAMP,
                    lease_expires_at = NULL, last_error = NULL
                WHERE id = ?
                """, row.id());
        } catch (RuntimeException exception) {
            recordFailure(row, exception);
        }
    }

    private void recordFailure(DeliveryRow row, RuntimeException exception) {
        boolean exhausted = row.attemptCount() >= maxAttempts;
        int delaySeconds = Math.min(3600, 5 << Math.min(row.attemptCount() - 1, 9));
        String error = exception.getMessage() == null
            ? exception.getClass().getSimpleName()
            : exception.getMessage();
        if (error.length() > MAX_ERROR_LENGTH) error = error.substring(0, MAX_ERROR_LENGTH);
        jdbcTemplate.update("""
            UPDATE notification_delivery_outbox
            SET status = ?, next_attempt_at = ?, lease_expires_at = NULL, last_error = ?
            WHERE id = ?
            """,
            exhausted ? "DEAD" : "PENDING",
            Timestamp.from(Instant.now().plusSeconds(delaySeconds)),
            error,
            row.id()
        );
        if (exhausted) {
            log.error("Notification email delivery {} exhausted {} attempts",
                row.id(), row.attemptCount(), exception);
        } else {
            log.warn("Notification email delivery {} attempt {} failed; retry scheduled in {} seconds",
                row.id(), row.attemptCount(), delaySeconds, exception);
        }
    }

    private record DeliveryRow(UUID id, UUID notificationId, int attemptCount) {}
}
