package com.syncdoc.collaboration.webhook.service;

import com.syncdoc.collaboration.webhook.dto.GithubEventPayload;
import org.springframework.stereotype.Component;

@Component
public class GithubEventSchemaValidator {

    private static final String HASH_REGEX = "^[a-fA-F0-9]{64}$";

    public boolean isValid(GithubEventPayload payload) {
        return payload != null
            && hasText(payload.eventId())
            && hasText(payload.eventType())
            && hasText(payload.repositoryId())
            && hasText(payload.deliveryId())
            && hasText(payload.schemaVersion())
            && payload.occurredAt() != null
            && payload.payloadHash() != null
            && payload.payloadHash().matches(HASH_REGEX);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
