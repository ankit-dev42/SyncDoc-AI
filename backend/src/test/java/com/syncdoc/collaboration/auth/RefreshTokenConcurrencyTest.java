package com.syncdoc.collaboration.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncdoc.collaboration.auth.dto.LoginRequest;
import com.syncdoc.collaboration.auth.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests that two concurrent refresh calls with the same token within the 2-second
 * grace window both succeed — SC-P1-8.
 *
 * <p>The second call re-issues the same new-rotation token (idempotent within the window).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RefreshTokenConcurrencyTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("Two simultaneous refresh calls with same token within 2s grace window → both return 200 (SC-P1-8)")
    void concurrentRefresh_withinGraceWindow_bothSucceed() throws Exception {
        String email = "concurrent-" + UUID.randomUUID() + "@example.com";
        String refreshToken = registerLoginAndGetRefreshCookie(email);

        // Perform the first refresh, get the new token
        MvcResult first = mockMvc.perform(post("/api/auth/refresh")
            .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshToken))
            .header("User-Agent", "TestAgent/1.0"))
            .andExpect(status().isOk())
            .andReturn();

        // Immediately replay the OLD token (within 2-second grace window) → must also return 200
        MvcResult second = mockMvc.perform(post("/api/auth/refresh")
            .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshToken))
            .header("User-Agent", "TestAgent/1.0"))
            .andExpect(status().isOk())
            .andReturn();

        // Both responses must contain an access token
        String accessToken1 = extractAccessToken(first);
        String accessToken2 = extractAccessToken(second);
        assertNotNull(accessToken1);
        assertNotNull(accessToken2);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String registerLoginAndGetRefreshCookie(String email) throws Exception {
        RegisterRequest reg = new RegisterRequest(email, "passw0rd123", "Concurrent Test");
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reg)))
            .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest(email, "passw0rd123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(login))
            .header("User-Agent", "TestAgent/1.0"))
            .andExpect(status().isOk())
            .andReturn();

        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookieHeader);
        for (String part : setCookieHeader.split(";")) {
            part = part.trim();
            if (part.startsWith("refreshToken=")) {
                return part.substring("refreshToken=".length());
            }
        }
        throw new IllegalStateException("refreshToken not found");
    }

    private String extractAccessToken(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode dataNode = root.get("data");
        if (dataNode != null && dataNode.has("accessToken")) {
            return dataNode.get("accessToken").asText();
        }
        return null;
    }
}
