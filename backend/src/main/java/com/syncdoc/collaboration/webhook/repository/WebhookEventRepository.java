package com.syncdoc.collaboration.webhook.repository;

import com.syncdoc.collaboration.webhook.model.WebhookEvent;
import com.syncdoc.collaboration.webhook.model.WebhookEvent.WebhookStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, String> {

    Optional<WebhookEvent> findByPayloadHash(String payloadHash);

    List<WebhookEvent> findByStatus(WebhookStatus status);
}