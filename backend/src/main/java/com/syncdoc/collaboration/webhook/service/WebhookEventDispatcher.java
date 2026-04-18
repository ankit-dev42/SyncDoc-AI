package com.syncdoc.collaboration.webhook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncdoc.collaboration.exception.BusinessValidationException;
import com.syncdoc.collaboration.webhook.dto.GithubEventPayload;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;

@Service
public class WebhookEventDispatcher {

    private final ObjectMapper objectMapper;
    private final GithubEventSchemaValidator schemaValidator;
    private final ApplicationEventPublisher eventPublisher;

    public WebhookEventDispatcher(
        ObjectMapper objectMapper,
        GithubEventSchemaValidator schemaValidator,
        ApplicationEventPublisher eventPublisher
    ) {
        this.objectMapper = objectMapper;
        this.schemaValidator = schemaValidator;
        this.eventPublisher = eventPublisher;
    }

    @Async
    public void dispatch(String deliveryId, String eventType, String payloadHash, String payload) {
        GithubEventPayload eventPayload = toEventPayload(deliveryId, eventType, payloadHash, payload);

        if (!schemaValidator.isValid(eventPayload)) {
            throw new BusinessValidationException(400, "INVALID_WEBHOOK_SCHEMA", "Webhook payload schema validation failed");
        }

        eventPublisher.publishEvent(eventPayload);
    }

    private GithubEventPayload toEventPayload(String deliveryId, String eventType, String payloadHash, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventId = root.path("id").asText(deliveryId);
            String repositoryId = root.path("repository").path("id").asText("unknown-repo");

            return new GithubEventPayload(
                eventId,
                eventType == null || eventType.isBlank() ? "unknown" : eventType,
                repositoryId,
                deliveryId,
                payloadHash,
                Instant.now(),
                "1.0"
            );
        } catch (IOException ex) {
            throw new BusinessValidationException(400, "MALFORMED_WEBHOOK_PAYLOAD", "Invalid webhook payload format");
        }
    }
}
