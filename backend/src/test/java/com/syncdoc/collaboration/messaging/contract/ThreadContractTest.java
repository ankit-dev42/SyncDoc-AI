package com.syncdoc.collaboration.messaging.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldValidateThreadReplyPayloadContract() throws Exception {
        Map<String, Object> payload = Map.of(
            "workspaceId", "ws-001",
            "channelId", "ch-123",
            "parentMessageId", "msg-root-001",
            "senderId", "user-007",
            "content", "I agree with this plan"
        );

        String serialized = objectMapper.writeValueAsString(payload);
        var node = objectMapper.readTree(serialized);

        assertThat(node.get("workspaceId").asText()).isEqualTo("ws-001");
        assertThat(node.get("channelId").asText()).isEqualTo("ch-123");
        assertThat(node.get("parentMessageId").asText()).isEqualTo("msg-root-001");
        assertThat(node.get("senderId").asText()).isEqualTo("user-007");
        assertThat(node.get("content").asText()).isNotBlank();
    }
}
