CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expiry ON refresh_tokens(expires_at);

CREATE TABLE user_invitations (
    id UUID PRIMARY KEY,
    email VARCHAR(180) NOT NULL,
    role VARCHAR(30) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    invited_by VARCHAR(180) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_invitations_email_expiry
    ON user_invitations(email, expires_at);

CREATE TABLE auth_audit_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(40) NOT NULL,
    actor_email VARCHAR(180),
    subject_email VARCHAR(180),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auth_audit_events_actor_time
    ON auth_audit_events(actor_email, occurred_at DESC);
