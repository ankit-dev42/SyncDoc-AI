-- V1__Create_tables.sql
-- Column types use VARCHAR(50) for all IDs to match JPA entity String fields.

-- Messages table
CREATE TABLE messages (
    id              VARCHAR(50)     NOT NULL,
    workspace_id    VARCHAR(50)     NOT NULL,
    channel_id      VARCHAR(50)     NOT NULL,
    sender_id       VARCHAR(50)     NOT NULL,
    content         VARCHAR(10000)  NOT NULL,
    sequence_number BIGINT          NOT NULL,
    idempotency_key VARCHAR(100),
    message_type    VARCHAR(50)     DEFAULT 'TEXT',
    parent_message_id VARCHAR(50),
    edited_at       TIMESTAMP,
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (channel_id, sequence_number),
    UNIQUE (idempotency_key)
);

CREATE INDEX idx_messages_workspace_channel ON messages(workspace_id, channel_id);
CREATE INDEX idx_messages_workspace_channel_sequence ON messages(workspace_id, channel_id, sequence_number);
CREATE INDEX idx_messages_sender ON messages(sender_id);

-- Presence table
CREATE TABLE presence (
    id           VARCHAR(50)  NOT NULL,
    workspace_id VARCHAR(50)  NOT NULL,
    user_id      VARCHAR(50)  NOT NULL,
    status       VARCHAR(16)  NOT NULL DEFAULT 'OFFLINE',
    manual_status VARCHAR(16),
    last_seen_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX idx_presence_workspace_user ON presence(workspace_id, user_id);
CREATE INDEX idx_presence_workspace_status ON presence(workspace_id, status);

-- Message threads table
CREATE TABLE message_threads (
    id              VARCHAR(50)  NOT NULL,
    workspace_id    VARCHAR(50)  NOT NULL,
    channel_id      VARCHAR(50)  NOT NULL,
    root_message_id VARCHAR(50)  NOT NULL,
    reply_count     BIGINT       NOT NULL DEFAULT 0,
    last_activity_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_thread_workspace_channel ON message_threads(workspace_id, channel_id);
CREATE UNIQUE INDEX idx_thread_root_message ON message_threads(root_message_id);