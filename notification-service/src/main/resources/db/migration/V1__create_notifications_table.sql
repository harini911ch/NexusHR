CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    recipient_email VARCHAR(255) NOT NULL,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    type VARCHAR(50) NOT NULL,
    related_entity_type VARCHAR(100),
    related_entity_id VARCHAR(100),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_notifications_recipient_created
    ON notifications (recipient_email, created_at DESC);

CREATE INDEX idx_notifications_recipient_unread
    ON notifications (recipient_email, is_read);
