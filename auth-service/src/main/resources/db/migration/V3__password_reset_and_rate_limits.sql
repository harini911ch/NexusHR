CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_password_reset_tokens_user
    ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_expiry
    ON password_reset_tokens(expires_at);

CREATE TABLE auth_rate_limit_buckets (
    bucket_hash VARCHAR(64) PRIMARY KEY,
    window_started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    request_count INTEGER NOT NULL CHECK (request_count > 0),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_auth_rate_limit_updated_at
    ON auth_rate_limit_buckets(updated_at);
