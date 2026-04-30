package com.syncdoc.collaboration.webhook.controller;

import com.syncdoc.collaboration.common.dto.ApiResponse;
import com.syncdoc.collaboration.config.BusinessValidationProperties;
import com.syncdoc.collaboration.exception.BusinessValidationException;
import com.syncdoc.collaboration.webhook.repository.WebhookEventRepository;
import com.syncdoc.collaboration.webhook.security.GitHubWebhookSignatureVerifier;
import com.syncdoc.collaboration.webhook.service.WebhookAuditService;
import com.syncdoc.collaboration.webhook.service.WebhookEventDispatcher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final GitHubWebhookSignatureVerifier signatureVerifier;
    private final WebhookEventDispatcher webhookEventDispatcher;
    private final WebhookAuditService webhookAuditService;
    private final BusinessValidationProperties businessValidationProperties;
    private final WebhookEventRepository webhookEventRepository;

    public WebhookController(
        GitHubWebhookSignatureVerifier signatureVerifier,
        WebhookEventDispatcher webhookEventDispatcher,
        WebhookAuditService webhookAuditService,
        BusinessValidationProperties businessValidationProperties,
        WebhookEventRepository webhookEventRepository
    ) {
        this.signatureVerifier = signatureVerifier;
        this.webhookEventDispatcher = webhookEventDispatcher;
        this.webhookAuditService = webhookAuditService;
        this.businessValidationProperties = businessValidationProperties;
        this.webhookEventRepository = webhookEventRepository;
    }

    @PostMapping("/github")
    public ResponseEntity<ApiResponse<Void>> receiveGithubWebhook(
        @RequestHeader("X-Hub-Signature-256") String signature,
        @RequestHeader(value = "X-GitHub-Delivery", required = false, defaultValue = "unknown-delivery") String deliveryId,
        @RequestHeader(value = "X-GitHub-Event", required = false, defaultValue = "unknown") String eventType,
        @RequestBody String payload
    ) {
        String payloadHash = sha256Hex(payload);
        String webhookSecret = businessValidationProperties.getGithub().getWebhookSecret();

        if (!signatureVerifier.isValid(signature, payload, webhookSecret)) {
            webhookAuditService.recordRejected(payloadHash, payload, "Invalid HMAC signature");
            throw new BusinessValidationException(403, "INVALID_WEBHOOK_SIGNATURE", "Webhook signature verification failed");
        }

        // Idempotency guard — T151: return 202 immediately if hash already exists
        if (webhookEventRepository.existsByPayloadHash(payloadHash)) {
            return ResponseEntity.accepted().body(ApiResponse.success("Webhook already processed", null));
        }

        webhookAuditService.recordAccepted(payloadHash, payload);
        webhookEventDispatcher.dispatch(deliveryId, eventType, payloadHash, payload);

        return ResponseEntity.accepted().body(ApiResponse.success("Webhook accepted", null));
    }

    private String sha256Hex(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash webhook payload", ex);
        }
    }
}
