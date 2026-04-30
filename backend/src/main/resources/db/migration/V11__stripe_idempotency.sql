-- T108: Stripe event idempotency table
CREATE TABLE processed_stripe_events (
    stripe_event_id VARCHAR(255) NOT NULL PRIMARY KEY,
    processed_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
