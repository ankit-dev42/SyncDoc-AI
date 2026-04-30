-- T106: Create sync_logs table for tracking GitHub→project synchronisation events
CREATE TABLE sync_logs (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    project_id    VARCHAR(100) NOT NULL REFERENCES projects (id),
    event_type    VARCHAR(50) NOT NULL,
    github_event_id VARCHAR(100),
    status        VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    started_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    completed_at  TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    metadata      JSONB
);

CREATE INDEX idx_sync_logs_project_id ON sync_logs (project_id);
CREATE INDEX idx_sync_logs_status     ON sync_logs (status);
CREATE INDEX idx_sync_logs_started_at ON sync_logs (started_at DESC);
