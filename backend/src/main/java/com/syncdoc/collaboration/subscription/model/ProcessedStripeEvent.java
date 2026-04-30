package com.syncdoc.collaboration.subscription.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "processed_stripe_events")
public class ProcessedStripeEvent {

    @Id
    @Column(name = "stripe_event_id", length = 255)
    private String stripeEventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public ProcessedStripeEvent() {
    }

    public ProcessedStripeEvent(String stripeEventId, Instant processedAt) {
        this.stripeEventId = stripeEventId;
        this.processedAt = processedAt;
    }

    public String getStripeEventId() {
        return stripeEventId;
    }

    public void setStripeEventId(String stripeEventId) {
        this.stripeEventId = stripeEventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
