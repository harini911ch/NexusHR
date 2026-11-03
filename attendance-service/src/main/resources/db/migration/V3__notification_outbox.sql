CREATE TABLE notification_outbox (
    id UUID PRIMARY KEY,
    recipient_email VARCHAR(255) NOT NULL,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    type VARCHAR(50) NOT NULL,
    related_entity_type VARCHAR(100),
    related_entity_id VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PROCESSING', 'DELIVERED', 'DEAD')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lease_expires_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(1000)
);

CREATE INDEX idx_notification_outbox_due
    ON notification_outbox (status, next_attempt_at, created_at);
