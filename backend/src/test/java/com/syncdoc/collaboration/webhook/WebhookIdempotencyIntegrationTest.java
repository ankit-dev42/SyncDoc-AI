package com.syncdoc.collaboration.webhook;

import com.syncdoc.collaboration.config.BusinessValidationProperties;
import com.syncdoc.collaboration.exception.GlobalExceptionHandler;
import com.syncdoc.collaboration.observability.AuditLogger;
import com.syncdoc.collaboration.webhook.controller.WebhookController;
import com.syncdoc.collaboration.webhook.repository.WebhookEventRepository;
import com.syncdoc.collaboration.webhook.security.GitHubWebhookSignatureVerifier;
import com.syncdoc.collaboration.webhook.service.WebhookAuditService;
import com.syncdoc.collaboration.webhook.service.WebhookEventDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Webhook idempotency tests")
class WebhookIdempotencyIntegrationTest {

    @Mock private GitHubWebhookSignatureVerifier signatureVerifier;
    @Mock private WebhookEventDispatcher webhookEventDispatcher;
    @Mock private WebhookAuditService webhookAuditService;
    @Mock private WebhookEventRepository webhookEventRepository;
    @Mock private AuditLogger auditLogger;

    private MockMvc mockMvc;
    private final String secret = "test-secret";

    @BeforeEach
    void setUp() {
        BusinessValidationProperties props = new BusinessValidationProperties();
        props.getGithub().setWebhookSecret(secret);
        WebhookController controller = new WebhookController(
            signatureVerifier, webhookEventDispatcher, webhookAuditService, props, webhookEventRepository, auditLogger);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(org.mockito.Mockito.mock(AuditLogger.class)))
            .build();
    }

    @Test
    @DisplayName("First delivery is dispatched normally")
    void firstDelivery_isDispatched() throws Exception {
        String payload = "{\"id\":\"evt-1\",\"repository\":{\"id\":\"repo-1\"}}";
        when(signatureVerifier.isValid(anyString(), anyString(), anyString())).thenReturn(true);
        when(webhookEventRepository.existsByPayloadHash(anyString())).thenReturn(false);

        mockMvc.perform(post("/api/v1/webhooks/github")
                .header("X-Hub-Signature-256", "sha256=dummy")
                .header("X-GitHub-Delivery", "delivery-1")
                .header("X-GitHub-Event", "push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isAccepted());

        verify(webhookEventDispatcher).dispatch(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Duplicate payload returns 202 without re-dispatching")
    void duplicateDelivery_returns202WithoutDispatch() throws Exception {
        String payload = "{\"id\":\"evt-1\",\"repository\":{\"id\":\"repo-1\"}}";
        when(signatureVerifier.isValid(anyString(), anyString(), anyString())).thenReturn(true);
        when(webhookEventRepository.existsByPayloadHash(anyString())).thenReturn(true);

        mockMvc.perform(post("/api/v1/webhooks/github")
                .header("X-Hub-Signature-256", "sha256=dummy")
                .header("X-GitHub-Delivery", "delivery-2")
                .header("X-GitHub-Event", "push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isAccepted());

        verify(webhookEventDispatcher, never()).dispatch(any(), any(), any(), any());
    }
}
