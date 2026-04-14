package com.syncdoc.collaboration.webhook.integration;

import com.syncdoc.collaboration.exception.BusinessValidationExceptionHandler;
import com.syncdoc.collaboration.webhook.controller.WebhookController;
import com.syncdoc.collaboration.webhook.security.GitHubWebhookSignatureVerifier;
import com.syncdoc.collaboration.webhook.service.WebhookAuditService;
import com.syncdoc.collaboration.webhook.service.WebhookEventDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WebhookControllerIntegrationTest {

    @Mock
    private GitHubWebhookSignatureVerifier signatureVerifier;

    @Mock
    private WebhookEventDispatcher webhookEventDispatcher;

    @Mock
    private WebhookAuditService webhookAuditService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        WebhookController controller = new WebhookController(signatureVerifier, webhookEventDispatcher, webhookAuditService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new BusinessValidationExceptionHandler())
            .build();
    }

    @Test
    void validSignatureShouldReturn202AndDispatch() throws Exception {
        String payload = "{\"id\":\"evt-1\",\"repository\":{\"id\":\"repo-1\"}}";

        when(signatureVerifier.isValid(any(), eq(payload), any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/webhooks/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=valid")
                .header("X-GitHub-Delivery", "delivery-1")
                .header("X-GitHub-Event", "push")
                .content(payload))
            .andExpect(status().isAccepted());

        verify(webhookEventDispatcher).dispatch(eq("delivery-1"), eq("push"), any(), eq(payload));
        verify(webhookAuditService).recordAccepted(any(), eq(payload));
    }

    @Test
    void invalidSignatureShouldReturn403AndSkipDispatch() throws Exception {
        String payload = "{\"id\":\"evt-1\"}";

        when(signatureVerifier.isValid(any(), eq(payload), any())).thenReturn(false);

        mockMvc.perform(post("/api/v1/webhooks/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=bad")
                .content(payload))
            .andExpect(status().isForbidden());

        verify(webhookAuditService).recordRejected(any(), eq(payload), eq("Invalid HMAC signature"));
        verify(webhookEventDispatcher, never()).dispatch(any(), any(), any(), any());
    }
}
