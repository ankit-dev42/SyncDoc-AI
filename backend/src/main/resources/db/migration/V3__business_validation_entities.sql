-- V3__business_validation_entities.sql
-- Business validation entities for subscription, project access, webhook audit, and AI extraction.

CREATE TABLE IF NOT EXISTS user_subscriptions (
    id                  VARCHAR(100) NOT NULL,
    user_id             VARCHAR(100) NOT NULL,
    subscription_tier   VARCHAR(20)  NOT NULL,
    stripe_customer_id  VARCHAR(100) NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    renewed_at          TIMESTAMP,
    expires_at          TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_subscriptions_user_id ON user_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_subscriptions_status ON user_subscriptions(status);

CREATE TABLE IF NOT EXISTS projects (
    id              VARCHAR(100) NOT NULL,
    owner_id        VARCHAR(100) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    access_control  VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_project_owner ON projects(owner_id);
CREATE INDEX IF NOT EXISTS idx_project_created ON projects(created_at);

CREATE TABLE IF NOT EXISTS webhook_events (
    id                VARCHAR(100) NOT NULL,
    source            VARCHAR(20)  NOT NULL,
    payload_hash      VARCHAR(64)  NOT NULL,
    payload_snapshot  TEXT,
    status            VARCHAR(20)  NOT NULL,
    rejection_reason  VARCHAR(255),
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dispatched_at     TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_webhook_source_status ON webhook_events(source, status);
CREATE INDEX IF NOT EXISTS idx_webhook_created ON webhook_events(created_at);

CREATE TABLE IF NOT EXISTS generated_documentation (
    id                 VARCHAR(100) NOT NULL,
    user_id            VARCHAR(100) NOT NULL,
    source_content_id  VARCHAR(50)  NOT NULL,
    source_content     TEXT,
    key_changes        TEXT,
    action_items       TEXT,
    status             VARCHAR(20)  NOT NULL,
    quality_score      DOUBLE PRECISION,
    processing_error   TEXT,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at       TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_generated_doc_user ON generated_documentation(user_id);
CREATE INDEX IF NOT EXISTS idx_generated_doc_status ON generated_documentation(status);