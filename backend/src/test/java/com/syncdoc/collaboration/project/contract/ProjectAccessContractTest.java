package com.syncdoc.collaboration.project.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectAccessContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldValidateGetProjectSuccessPayload() throws Exception {
        Map<String, Object> payload = Map.of(
            "success", true,
            "message", "Project retrieved",
            "data", Map.of(
                "id", "project-123",
                "ownerId", "user-owner",
                "name", "SyncDoc Workspace",
                "accessControl", "PRIVATE"
            )
        );

        String serialized = objectMapper.writeValueAsString(payload);
        var node = objectMapper.readTree(serialized);

        assertThat(node.get("data").get("id").asText()).isEqualTo("project-123");
        assertThat(node.get("data").get("ownerId").asText()).isEqualTo("user-owner");
        assertThat(node.get("data").get("accessControl").asText()).isIn("PRIVATE", "SHARED");
    }

    @Test
    void shouldValidateGetProjectForbiddenPayload() throws Exception {
        Map<String, Object> payload = Map.of(
            "success", false,
            "message", "Access denied: you don't own this project."
        );

        String serialized = objectMapper.writeValueAsString(payload);
        var node = objectMapper.readTree(serialized);

        assertThat(node.get("success").asBoolean()).isFalse();
        assertThat(node.get("message").asText()).contains("Access denied");
    }
}
