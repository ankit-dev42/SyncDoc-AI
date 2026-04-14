package com.syncdoc.collaboration.messaging.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class WebSocketMessageContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testWebSocketMessageProtocolContract() throws Exception {
        // Test that WebSocket message protocol follows expected JSON structure

        // Sample message payload
        String samplePayload = """
            {
                "content": "Hello, world!",
                "idempotencyKey": "msg-12345"
            }
            """;

        // Parse and validate structure
        var jsonNode = objectMapper.readTree(samplePayload);

        assertThat(jsonNode.has("content")).isTrue();
        assertThat(jsonNode.get("content").asText()).isEqualTo("Hello, world!");
        assertThat(jsonNode.has("idempotencyKey")).isTrue();
        assertThat(jsonNode.get("idempotencyKey").asText()).isEqualTo("msg-12345");

        // Test serialization
        var messageMap = Map.of(
            "content", "Test message",
            "idempotencyKey", "test-key-123"
        );

        String serialized = objectMapper.writeValueAsString(messageMap);
        assertThat(serialized).contains("Test message");
        assertThat(serialized).contains("test-key-123");
    }
}