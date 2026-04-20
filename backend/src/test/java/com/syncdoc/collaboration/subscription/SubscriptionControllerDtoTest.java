package com.syncdoc.collaboration.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncdoc.collaboration.security.JwtTestTokenHelper;
import com.syncdoc.collaboration.subscription.controller.SubscriptionController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies SubscriptionTierResponse shape: must contain {@code tier, status, expiresAt}
 * and must NOT contain {@code stripeCustomerId} or {@code userId} (SC-P1-4).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubscriptionControllerDtoTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("SubscriptionTierResponse record has exactly {tier, status, expiresAt} — no stripeCustomerId, no userId")
    void subscriptionTierResponse_fieldsAreWhitelisted() {
        Class<?> responseClass = SubscriptionController.SubscriptionTierResponse.class;
        assertTrue(responseClass.isRecord(),
            "SubscriptionTierResponse must be a record");

        Set<String> componentNames = Arrays.stream(responseClass.getRecordComponents())
            .map(RecordComponent::getName)
            .collect(Collectors.toSet());

        // required fields
        assertTrue(componentNames.contains("tier"), "Must contain 'tier'");
        assertTrue(componentNames.contains("status"), "Must contain 'status'");
        assertTrue(componentNames.contains("expiresAt"), "Must contain 'expiresAt'");

        // forbidden fields
        assertFalse(componentNames.contains("stripeCustomerId"),
            "stripeCustomerId MUST NOT be in SubscriptionTierResponse (security leak)");
        assertFalse(componentNames.contains("userId"),
            "userId MUST NOT be in SubscriptionTierResponse");
    }

    @Test
    @DisplayName("GET /api/v1/subscriptions/{userId}/tier response body does not contain stripeCustomerId")
    void getTier_responseDoesNotLeakStripeCustomerId() throws Exception {
        String userId = UUID.randomUUID().toString();
        String token = JwtTestTokenHelper.signedToken(userId);

        String responseBody = mockMvc.perform(
                get("/api/v1/subscriptions/{userId}/tier", userId)
                    .header("Authorization", "Bearer " + token)
                    .header("X-Workspace-Id", "ws-test"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode data = root.path("data");

        assertFalse(data.has("stripeCustomerId"),
            "stripeCustomerId MUST NOT be present in the API response");
        assertFalse(data.has("userId"),
            "userId MUST NOT be present in the API response");
        assertTrue(data.has("tier"), "Response must include 'tier'");
        assertTrue(data.has("status"), "Response must include 'status'");
    }
}
