package com.syncdoc.collaboration.ai.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AIExtractionContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldValidateGetExtractionStatusPayload() throws Exception {
        Map<String, Object> payload = Map.of(
            "success", true,
            "message", "Extraction status retrieved",
            "data", Map.of(
                "docId", "doc-123",
                "status", "COMPLETED"
            )
        );

        String serialized = objectMapper.writeValueAsString(payload);
        var node = objectMapper.readTree(serialized);

        assertThat(node.get("data").get("docId").asText()).isEqualTo("doc-123");
        assertThat(node.get("data").get("status").asText())
            .isIn("PENDING", "COMPLETED", "FAILED");
    }

    @Test
    void shouldValidateGetExtractionResultPayload() throws Exception {
        Map<String, Object> payload = Map.of(
            "success", true,
            "message", "Extraction result retrieved",
            "data", Map.of(
                "docId", "doc-123",
                "status", "COMPLETED",
                "keyChanges", "- Added OAuth login",
                "actionItems", "- Update the docs",
                "qualityScore", 1.0
            )
        );

        String serialized = objectMapper.writeValueAsString(payload);
        var node = objectMapper.readTree(serialized);

        assertThat(node.get("data").get("docId").asText()).isEqualTo("doc-123");
        assertThat(node.get("data").get("status").asText()).isEqualTo("COMPLETED");
        assertThat(node.get("data").get("keyChanges").asText()).contains("OAuth");
        assertThat(node.get("data").get("actionItems").asText()).contains("docs");
        assertThat(node.get("data").get("qualityScore").asDouble()).isEqualTo(1.0d);
    }
}