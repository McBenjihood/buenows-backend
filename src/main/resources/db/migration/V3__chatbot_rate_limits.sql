CREATE TABLE IF NOT EXISTS chatbot_rate_limits
(
    limit_key     VARCHAR(255) PRIMARY KEY,
    request_count INTEGER NOT NULL DEFAULT 0,
    reset_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_chatbot_rate_limits_reset_at
    ON chatbot_rate_limits (reset_at);
