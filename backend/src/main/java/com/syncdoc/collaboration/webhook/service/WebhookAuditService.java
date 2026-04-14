package com.syncdoc.collaboration.webhook.service;

import com.syncdoc.collaboration.webhook.model.WebhookEvent;
import com.syncdoc.collaboration.webhook.model.WebhookEvent.WebhookSource;
import com.syncdoc.collaboration.webhook.model.WebhookEvent.WebhookStatus;
import com.syncdoc.collaboration.webhook.repository.WebhookEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class WebhookAuditService {

    private final WebhookEventRepository webhookEventRepository;

    public WebhookAuditService(WebhookEventRepository webhookEventRepository) {
        this.webhookEventRepository = webhookEventRepository;
    }

    public void recordAccepted(String payloadHash, String payloadSnapshot) {
        WebhookEvent event = new WebhookEvent();
        event.setSource(WebhookSource.GITHUB);
        event.setPayloadHash(payloadHash);
        event.setPayloadSnapshot(payloadSnapshot);
        event.setStatus(WebhookStatus.ACCEPTED);
        event.setDispatchedAt(Instant.now());
        webhookEventRepository.save(event);
    }

    public void recordRejected(String payloadHash, String payloadSnapshot, String reason) {
        WebhookEvent event = new WebhookEvent();
        event.setSource(WebhookSource.GITHUB);
        event.setPayloadHash(payloadHash);
        event.setPayloadSnapshot(payloadSnapshot);
        event.setStatus(WebhookStatus.REJECTED);
        event.setRejectionReason(reason);
        webhookEventRepository.save(event);
    }
}
