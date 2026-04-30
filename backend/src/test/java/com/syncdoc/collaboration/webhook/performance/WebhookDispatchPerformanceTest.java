package com.syncdoc.collaboration.webhook.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncdoc.collaboration.config.BusinessValidationProperties;
import com.syncdoc.collaboration.webhook.controller.WebhookController;
import com.syncdoc.collaboration.webhook.security.GitHubWebhookSignatureVerifier;
import com.syncdoc.collaboration.webhook.service.GithubEventSchemaValidator;
import com.syncdoc.collaboration.webhook.service.WebhookAuditService;
import com.syncdoc.collaboration.webhook.service.WebhookEventDispatcher;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.syncdoc.collaboration.webhook.repository.WebhookEventRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Tag("performance")
class WebhookDispatchPerformanceTest {

    @Test
    void p95ReceiptToPublishLatencyShouldStayWithin100ms() {
        String secret = "phase8-webhook-secret";
        GitHubWebhookSignatureVerifier signatureVerifier = new GitHubWebhookSignatureVerifier();
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        WebhookAuditService auditService = mock(WebhookAuditService.class);
        WebhookEventRepository eventRepository = mock(WebhookEventRepository.class);
        WebhookEventDispatcher dispatcher = new WebhookEventDispatcher(new ObjectMapper(), new GithubEventSchemaValidator(), publisher, auditService);

        BusinessValidationProperties properties = new BusinessValidationProperties();
        properties.getGithub().setWebhookSecret(secret);

        WebhookController controller = new WebhookController(signatureVerifier, dispatcher, auditService, properties, eventRepository);
        List<Long> latencies = new ArrayList<>();
        int samples = 100;

        for (int i = 0; i < samples; i++) {
            String payload = "{\"id\":\"evt-" + i + "\",\"repository\":{\"id\":\"repo-" + i + "\"}}";
            String signature = "sha256=" + computeHexDigest(payload, secret);

            long start = System.nanoTime();
            var response = controller.receiveGithubWebhook(signature, "delivery-" + i, "push", payload);
            latencies.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));

            assertThat(response.getStatusCode().value()).isEqualTo(202);
        }

        latencies.sort(Long::compareTo);
        long p95 = latencies.get((int) Math.ceil(samples * 0.95) - 1);

        assertThat(p95)
            .as("p95 webhook receipt-to-publish latency across %d samples", samples)
            .isLessThanOrEqualTo(100);
    }

    private String computeHexDigest(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate webhook signature for test", ex);
        }
    }
}