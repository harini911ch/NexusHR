CREATE TABLE performance_feedback_invitations (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES performance_reviews(id) ON DELETE CASCADE,
    invitee_email VARCHAR(180) NOT NULL,
    relationship VARCHAR(50) NOT NULL,
    created_by VARCHAR(180) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_feedback_invitation_review_invitee
        UNIQUE (review_id, invitee_email)
);

CREATE INDEX idx_feedback_invitation_invitee
    ON performance_feedback_invitations(invitee_email, submitted_at);
