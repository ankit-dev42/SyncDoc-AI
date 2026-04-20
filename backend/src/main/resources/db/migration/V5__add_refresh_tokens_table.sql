-- V5__add_refresh_tokens_table.sql
-- Adds the refresh_tokens table for secure JWT refresh token rotation (Phase 1).
-- hashed_token stores SHA-256 hex digest (64 chars) — raw token is NEVER persisted.
-- replaced_at supports the < 2s grace window for concurrent mobile requests (AD-005).
-- bound_user_agent and bound_ip enable per-device token binding (FR-007).

CREATE TABLE refresh_tokens (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    hashed_token        VARCHAR(64)     NOT NULL,
    expires_at          TIMESTAMP       NOT NULL,
    revoked             BOOLEAN         NOT NULL DEFAULT FALSE,
    replaced_at         TIMESTAMP,
    bound_user_agent    VARCHAR(500),
    bound_ip            VARCHAR(45),
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_refresh_tokens_hashed ON refresh_tokens(hashed_token);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
