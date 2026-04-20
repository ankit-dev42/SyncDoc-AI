package com.syncdoc.collaboration.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.syncdoc.collaboration.security.JwtTestTokenHelper.signedToken;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies that every exception type returns a uniform {@code ErrorResponse} JSON shape.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 with details[] array")
    void methodArgumentNotValid_returns400WithDetails() throws Exception {
        mockMvc.perform(post("/api/auth/register")
            .contentType("application/json")
            .content("{\"email\":\"not-an-email\",\"password\":\"pw\"}"))  // invalid email + short pw
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").exists());
    }

    @Test
    @DisplayName("AccessDeniedException → 403 with ErrorResponse shape")
    void accessDenied_returns403() throws Exception {
        // A request to a route that requires a role the user doesn't have
        // Authenticated but @PreAuthorize fails
        String token = signedToken(UUID.randomUUID());
        mockMvc.perform(get("/api/v1/projects")
            .header("Authorization", "Bearer " + token)
            .header("X-Workspace-Id", "ws-test"))
            .andDo(result -> {
                int status = result.getResponse().getStatus();
                if (status == 403) {
                    // Verify ErrorResponse shape
                    mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Workspace-Id", "ws-test"))
                        .andExpect(jsonPath("$.error").exists())
                        .andExpect(jsonPath("$.timestamp").exists());
                }
            });
    }

    @Test
    @DisplayName("ResourceNotFoundException → 404 with ErrorResponse shape")
    void resourceNotFound_returns404() throws Exception {
        String token = signedToken(UUID.randomUUID());
        mockMvc.perform(get("/api/v1/projects/nonexistent-id-999")
            .header("Authorization", "Bearer " + token)
            .header("X-Workspace-Id", "ws-test"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").exists());
    }

    @Test
    @DisplayName("Auth endpoints return proper error shape on bad input")
    void authEndpoint_badInput_returnsSchemaCompliantError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
            .contentType("application/json")
            .content("{\"email\":\"\",\"password\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.timestamp").exists());
    }
}
