package com.syncdoc.collaboration.integration;

import com.syncdoc.collaboration.AbstractIntegrationTest;
import com.syncdoc.collaboration.webhook.model.WebhookEvent;
import com.syncdoc.collaboration.webhook.repository.WebhookEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testcontainers integration test: verifies WebhookEventRepository unique constraint on
 * payload_hash (T305).
 *
 * <p>NOT @Transactional — we need the first save to commit so the DB constraint fires on
 * the second duplicate insert. Spring wraps the second save in a separate transaction which
 * rolls back on constraint violation.
 *
 * <p>Runs only during {@code mvn verify} (Failsafe — see T300 pom.xml configuration).
 */
@SpringBootTest
@ActiveProfiles("test")
class WebhookEventRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Test
    @DisplayName("duplicate payloadHash must raise DataIntegrityViolationException")
    void duplicatePayloadHashThrowsException() {
        String payloadHash = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";

        WebhookEvent first = buildEvent(payloadHash);
        webhookEventRepository.saveAndFlush(first);

        WebhookEvent duplicate = buildEvent(payloadHash);
        assertThatThrownBy(() -> webhookEventRepository.saveAndFlush(duplicate))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private WebhookEvent buildEvent(String payloadHash) {
        WebhookEvent event = new WebhookEvent();
        event.setPayloadHash(payloadHash);
        event.setSource(WebhookEvent.WebhookSource.GITHUB);
        event.setStatus(WebhookEvent.WebhookStatus.ACCEPTED);
        event.setPayloadSnapshot("{\"test\":true}");
        return event;
    }
}
