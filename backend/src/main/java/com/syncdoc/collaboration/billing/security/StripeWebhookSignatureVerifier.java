package com.syncdoc.collaboration.billing.security;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StripeWebhookSignatureVerifier {

    private final String webhookSecret;

    public StripeWebhookSignatureVerifier(
        @Value("${integrations.stripe.webhook-secret:}") String webhookSecret
    ) {
        this.webhookSecret = webhookSecret;
    }

    public Event constructEvent(String payload, String sigHeader) throws SignatureVerificationException {
        return Webhook.constructEvent(payload, sigHeader, webhookSecret);
    }
}
