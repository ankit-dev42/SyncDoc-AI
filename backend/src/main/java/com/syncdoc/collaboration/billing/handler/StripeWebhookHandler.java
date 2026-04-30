package com.syncdoc.collaboration.billing.handler;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.syncdoc.collaboration.billing.security.StripeWebhookSignatureVerifier;
import com.syncdoc.collaboration.subscription.model.ProcessedStripeEvent;
import com.syncdoc.collaboration.subscription.repository.ProcessedStripeEventRepository;
import com.syncdoc.collaboration.subscription.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Component
public class StripeWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookHandler.class);

    private static final Set<String> HANDLED_EVENTS = Set.of(
        "customer.subscription.created",
        "customer.subscription.updated",
        "customer.subscription.deleted"
    );

    private final StripeWebhookSignatureVerifier signatureVerifier;
    private final ProcessedStripeEventRepository processedStripeEventRepository;
    private final SubscriptionService subscriptionService;

    public StripeWebhookHandler(
        StripeWebhookSignatureVerifier signatureVerifier,
        ProcessedStripeEventRepository processedStripeEventRepository,
        SubscriptionService subscriptionService
    ) {
        this.signatureVerifier = signatureVerifier;
        this.processedStripeEventRepository = processedStripeEventRepository;
        this.subscriptionService = subscriptionService;
    }

    public void handle(String payload, String sigHeader) throws SignatureVerificationException {
        // Rule 02: constructEvent is FIRST — validates signature before any other logic
        Event event = signatureVerifier.constructEvent(payload, sigHeader);

        String eventId = event.getId();

        // Idempotency: skip already-processed events
        if (processedStripeEventRepository.existsByStripeEventId(eventId)) {
            log.info("Skipping duplicate Stripe event id={}", eventId);
            return;
        }

        String eventType = event.getType();
        if (HANDLED_EVENTS.contains(eventType)) {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            Optional<StripeObject> stripeObject = deserializer.getObject();
            stripeObject.ifPresent(obj -> subscriptionService.invalidateCache(extractUserId(event)));
        }

        // Mark event as processed
        processedStripeEventRepository.save(new ProcessedStripeEvent(eventId, Instant.now()));
        log.info("Processed Stripe event id={} type={}", eventId, eventType);
    }

    private String extractUserId(Event event) {
        var metadata = event.getDataObjectDeserializer().getObject()
            .map(obj -> {
                if (obj instanceof com.stripe.model.Subscription sub) {
                    return sub.getMetadata() != null ? sub.getMetadata().get("userId") : null;
                }
                return null;
            })
            .orElse(null);
        return metadata;
    }
}
