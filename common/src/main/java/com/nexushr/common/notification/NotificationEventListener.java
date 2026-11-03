package com.nexushr.common.notification;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

public class NotificationEventListener {

    private final JdbcTemplate jdbcTemplate;

    NotificationEventListener(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void persist(NotificationEvent event) {
        jdbcTemplate.update("""
            INSERT INTO notification_outbox (
                id, recipient_email, title, message, type,
                related_entity_type, related_entity_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            UUID.randomUUID(),
            event.recipientEmail(),
            event.title(),
            event.message(),
            event.type(),
            event.relatedEntityType(),
            event.relatedEntityId()
        );
    }
}
