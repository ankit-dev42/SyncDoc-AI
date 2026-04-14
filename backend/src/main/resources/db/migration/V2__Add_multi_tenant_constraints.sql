-- V2__Add_multi_tenant_constraints.sql
-- Tenancy tables and FK constraints. All ID columns use VARCHAR(50) to match JPA String fields.

CREATE TABLE IF NOT EXISTS workspaces (
    id         VARCHAR(50)  NOT NULL,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS channels (
    id           VARCHAR(50)  NOT NULL,
    workspace_id VARCHAR(50)  NOT NULL,
    name         VARCHAR(255) NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, workspace_id),
    CONSTRAINT fk_channels_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id)
);

ALTER TABLE messages
    ADD CONSTRAINT fk_messages_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id);

ALTER TABLE messages
    ADD CONSTRAINT fk_messages_channel_workspace
    FOREIGN KEY (channel_id, workspace_id) REFERENCES channels(id, workspace_id);

ALTER TABLE presence
    ADD CONSTRAINT fk_presence_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id);

ALTER TABLE message_threads
    ADD CONSTRAINT fk_threads_workspace
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id);
