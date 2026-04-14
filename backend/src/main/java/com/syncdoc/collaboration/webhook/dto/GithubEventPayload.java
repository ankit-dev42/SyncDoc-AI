package com.syncdoc.collaboration.webhook.dto;

import java.time.Instant;

public record GithubEventPayload(
    String eventId,
    String eventType,
    String repositoryId,
    String deliveryId,
    String payloadHash,
    Instant occurredAt,
    String schemaVersion
) {
}
