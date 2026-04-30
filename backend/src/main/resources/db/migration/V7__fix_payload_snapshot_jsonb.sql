-- T104: Fix webhook_events payload_snapshot to JSONB and add error_message
ALTER TABLE webhook_events
    ALTER COLUMN payload_snapshot TYPE JSONB USING payload_snapshot::JSONB;

ALTER TABLE webhook_events
    ADD COLUMN IF NOT EXISTS error_message TEXT;
