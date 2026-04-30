-- T105: Unique index on webhook_events payload_hash for idempotency
-- Remove any duplicate rows first (keep lowest id per hash) to allow unique index creation
DELETE FROM webhook_events
WHERE id NOT IN (
    SELECT MIN(id)
    FROM webhook_events
    WHERE payload_hash IS NOT NULL
    GROUP BY payload_hash
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_webhook_events_payload_hash
    ON webhook_events (payload_hash);
