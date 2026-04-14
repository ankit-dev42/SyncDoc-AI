package com.syncdoc.collaboration.search.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SearchContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldValidateSearchResponseContract() throws Exception {
        Map<String, Object> result = Map.of(
            "messageId", "msg-123",
            "workspaceId", "ws-001",
            "channelId", "ch-007",
            "senderId", "user-42",
            "snippet", "...database architecture decision...",
            "createdAt", "2026-04-14T10:15:00Z"
        );

        String serialized = objectMapper.writeValueAsString(List.of(result));
        var node = objectMapper.readTree(serialized);

        assertThat(node.isArray()).isTrue();
        assertThat(node.get(0).get("messageId").asText()).isEqualTo("msg-123");
        assertThat(node.get(0).get("workspaceId").asText()).isEqualTo("ws-001");
        assertThat(node.get(0).get("channelId").asText()).isEqualTo("ch-007");
        assertThat(node.get(0).get("snippet").asText()).contains("database architecture");
    }
}
