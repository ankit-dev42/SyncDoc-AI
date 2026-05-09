package com.syncdoc.collaboration.billing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncdoc.collaboration.billing.dto.CreateCheckoutRequest;
import com.syncdoc.collaboration.billing.handler.StripeWebhookHandler;
import com.syncdoc.collaboration.exception.GlobalExceptionHandler;
import com.syncdoc.collaboration.subscription.client.StripeClient;
import com.syncdoc.collaboration.subscription.model.UserSubscription;
import com.syncdoc.collaboration.subscription.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingController integration tests")
class BillingControllerIntegrationTest {

    @Mock
    private StripeClient stripeClient;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private StripeWebhookHandler stripeWebhookHandler;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        BillingController controller = new BillingController(stripeClient, subscriptionService, stripeWebhookHandler);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(org.mockito.Mockito.mock(com.syncdoc.collaboration.observability.AuditLogger.class)))
            .build();
    }

    @Test
    @DisplayName("POST /checkout returns checkoutUrl from Stripe")
    void createCheckout_returnsStripeUrl() throws Exception {
        when(stripeClient.createCheckoutSession(anyString(), anyString()))
            .thenReturn(Optional.of(new StripeClient.CheckoutSessionResult(
                "https://checkout.stripe.com/pay/cs_test_abc123", "cs_test_abc123")));

        mockMvc.perform(post("/api/v1/billing/checkout")
                .principal(new UsernamePasswordAuthenticationToken("user-1", null, List.of()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateCheckoutRequest("price_test123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.checkoutUrl").value("https://checkout.stripe.com/pay/cs_test_abc123"));
    }

    @Test
    @DisplayName("POST /checkout returns 503 when Stripe returns empty")
    void createCheckout_stripeEmpty_returns503() throws Exception {
        when(stripeClient.createCheckoutSession(anyString(), anyString())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/billing/checkout")
                .principal(new UsernamePasswordAuthenticationToken("user-1", null, List.of()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateCheckoutRequest("price_test123"))))
            .andExpect(status().isServiceUnavailable());
    }
}
