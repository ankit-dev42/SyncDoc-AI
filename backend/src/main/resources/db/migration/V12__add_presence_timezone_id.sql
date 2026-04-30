-- Add IANA timezone identifier column to presence table (missing from V1)
ALTER TABLE presence
    ADD COLUMN IF NOT EXISTS timezone_id VARCHAR(64);
