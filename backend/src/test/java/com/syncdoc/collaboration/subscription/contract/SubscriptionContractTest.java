package com.syncdoc.collaboration.subscription.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncdoc.collaboration.subscription.controller.SubscriptionController;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldValidateGetTierResponseContract() throws Exception {
        Map<String, Object> payload = Map.of(
            "success", true,
            "message", "Subscription tier retrieved",
            "data", Map.of(
                "userId", "user-123",
                "tier", "PRO",
                "status", "ACTIVE",
                "stripeCustomerId", "cus_123"
            )
        );

        String serialized = objectMapper.writeValueAsString(payload);
        var node = objectMapper.readTree(serialized);

        assertThat(SubscriptionController.SUBSCRIPTION_TIER_PATH)
            .isEqualTo("/api/v1/subscriptions/{userId}/tier");
        assertThat(node.get("data").get("tier").asText()).isIn("FREE", "PRO", "ENTERPRISE");
        assertThat(node.get("data").get("status").asText()).isIn("ACTIVE", "CANCELED", "EXPIRED", "PAST_DUE");
    }

    @Test
    void shouldValidateCanSyncResponseContract() throws Exception {
        Map<String, Object> payload = Map.of(
            "success", true,
            "message", "Sync authorization evaluated",
            "data", Map.of(
                "authorized", false,
                "reason", "Free tier limit exceeded; upgrade required",
                "recommendedAction", "UPGRADE"
            )
        );

        String serialized = objectMapper.writeValueAsString(payload);
        var node = objectMapper.readTree(serialized);

        assertThat(SubscriptionController.SUBSCRIPTION_CAN_SYNC_PATH)
            .isEqualTo("/api/v1/subscriptions/{userId}/can-sync");
        assertThat(node.get("data").get("authorized").asBoolean()).isFalse();
        assertThat(node.get("data").get("reason").asText()).isNotBlank();
    }
}
