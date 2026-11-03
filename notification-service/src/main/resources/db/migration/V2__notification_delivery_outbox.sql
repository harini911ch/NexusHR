ALTER TABLE notifications
    ADD COLUMN source_event_id UUID;

CREATE UNIQUE INDEX uq_notifications_source_event
    ON notifications (source_event_id);

CREATE TABLE notification_delivery_outbox (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL CHECK (channel = 'EMAIL'),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PROCESSING', 'DELIVERED', 'DEAD')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lease_expires_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(1000),
    CONSTRAINT uq_notification_delivery_channel UNIQUE (notification_id, channel)
);

CREATE INDEX idx_notification_delivery_due
    ON notification_delivery_outbox (status, next_attempt_at, created_at);
