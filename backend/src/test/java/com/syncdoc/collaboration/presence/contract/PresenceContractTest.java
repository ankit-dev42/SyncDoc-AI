package com.syncdoc.collaboration.presence.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PresenceContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldValidatePresenceStatusPayloadContract() throws Exception {
        Map<String, Object> payload = Map.of(
            "workspaceId", "ws-001",
            "userId", "user-123",
            "status", "ONLINE",
            "lastSeenEpochMs", 1713076200000L
        );

        String serialized = objectMapper.writeValueAsString(payload);
        var node = objectMapper.readTree(serialized);

        assertThat(node.get("workspaceId").asText()).isEqualTo("ws-001");
        assertThat(node.get("userId").asText()).isEqualTo("user-123");
        assertThat(node.get("status").asText()).isIn("ONLINE", "AWAY", "OFFLINE");
        assertThat(node.get("lastSeenEpochMs").asLong()).isPositive();
    }
}
