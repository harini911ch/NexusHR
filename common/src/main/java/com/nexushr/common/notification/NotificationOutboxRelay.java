package com.nexushr.common.notification;

import java.net.http.HttpClient;
import java.time.Duration;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

public class NotificationOutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxRelay.class);
    private static final int MAX_ERROR_LENGTH = 1000;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final RestClient restClient;
    private final String notificationServiceUrl;
    private final String internalToken;
    private final int batchSize;
    private final int maxAttempts;
    private final int leaseSeconds;

    NotificationOutboxRelay(
        JdbcTemplate jdbcTemplate,
        TransactionTemplate transactionTemplate,
        RestClient.Builder builder,
        String notificationServiceUrl,
        String internalToken,
        int batchSize,
        int maxAttempts,
        int leaseSeconds
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        var requestFactory = new JdkClientHttpRequestFactory(client);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = builder.requestFactory(requestFactory).build();
        this.notificationServiceUrl = notificationServiceUrl;
        this.internalToken = internalToken;
        this.batchSize = Math.max(1, batchSize);
        this.maxAttempts = Math.max(1, maxAttempts);
        this.leaseSeconds = Math.max(5, leaseSeconds);
    }

    @Scheduled(fixedDelayString = "${app.notifications.outbox.poll-interval-ms:1000}")
    public void relayDueEvents() {
        List<OutboxRow> claimed = transactionTemplate.execute(status -> jdbcTemplate.query("""
            WITH due AS (
                SELECT id
                FROM notification_outbox
                WHERE (status = 'PENDING' AND next_attempt_at <= CURRENT_TIMESTAMP)
                   OR (status = 'PROCESSING' AND lease_expires_at < CURRENT_TIMESTAMP)
                ORDER BY created_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            UPDATE notification_outbox AS item
            SET status = 'PROCESSING',
                attempt_count = item.attempt_count + 1,
                lease_expires_at = CURRENT_TIMESTAMP + (? * INTERVAL '1 second')
            FROM due
            WHERE item.id = due.id
            RETURNING item.id, item.recipient_email, item.title, item.message, item.type,
                      item.related_entity_type, item.related_entity_id, item.attempt_count
            """, (rs, row) -> new OutboxRow(
                rs.getObject("id", UUID.class),
                rs.getString("recipient_email"),
                rs.getString("title"),
                rs.getString("message"),
                rs.getString("type"),
                rs.getString("related_entity_type"),
                rs.getString("related_entity_id"),
                rs.getInt("attempt_count")
            ), batchSize, leaseSeconds));
        if (claimed == null) return;

        for (OutboxRow row : claimed) {
            deliver(row);
        }
    }

    private void deliver(OutboxRow row) {
        try {
            if (internalToken.isBlank()) {
                throw new IllegalStateException(
                    "APP_NOTIFICATIONS_INTERNAL_TOKEN is not configured"
                );
            }
            restClient.post()
                .uri(notificationServiceUrl + "/api/notifications/internal/events")
                .header("X-Internal-Token", internalToken)
                .header("X-Notification-Event-Id", row.id().toString())
                .body(new NotificationEvent(
                    row.recipientEmail(), row.title(), row.message(), row.type(),
                    row.relatedEntityType(), row.relatedEntityId()
                ))
                .retrieve()
                .toBodilessEntity();
            jdbcTemplate.update("""
                UPDATE notification_outbox
                SET status = 'DELIVERED', delivered_at = CURRENT_TIMESTAMP,
                    lease_expires_at = NULL, last_error = NULL
                WHERE id = ?
                """, row.id());
        } catch (RuntimeException exception) {
            recordFailure(row, exception);
        }
    }

    private void recordFailure(OutboxRow row, RuntimeException exception) {
        boolean exhausted = row.attemptCount() >= maxAttempts;
        int delaySeconds = Math.min(3600, 5 << Math.min(row.attemptCount() - 1, 9));
        String error = exception.getMessage() == null
            ? exception.getClass().getSimpleName()
            : exception.getMessage();
        if (error.length() > MAX_ERROR_LENGTH) error = error.substring(0, MAX_ERROR_LENGTH);
        jdbcTemplate.update("""
            UPDATE notification_outbox
            SET status = ?, next_attempt_at = ?, lease_expires_at = NULL, last_error = ?
            WHERE id = ?
            """,
            exhausted ? "DEAD" : "PENDING",
            Timestamp.from(Instant.now().plusSeconds(delaySeconds)),
            error,
            row.id()
        );
        if (exhausted) {
            log.error("Notification outbox event {} exhausted {} delivery attempts",
                row.id(), row.attemptCount(), exception);
        } else {
            log.warn("Notification outbox event {} delivery attempt {} failed; retrying in {} seconds",
                row.id(), row.attemptCount(), delaySeconds, exception);
        }
    }

    private record OutboxRow(
        UUID id,
        String recipientEmail,
        String title,
        String message,
        String type,
        String relatedEntityType,
        String relatedEntityId,
        int attemptCount
    ) {}
}
