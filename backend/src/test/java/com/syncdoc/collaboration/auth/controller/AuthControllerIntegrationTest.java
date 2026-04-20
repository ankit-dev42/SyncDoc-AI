package com.syncdoc.collaboration.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncdoc.collaboration.auth.dto.LoginRequest;
import com.syncdoc.collaboration.auth.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack integration tests for AuthController covering register, login,
 * refresh, and logout flows (covers SC-P1-5).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("Register with valid data → 201, response has id/email/displayName, no password_hash")
    void register_success() throws Exception {
        RegisterRequest req = new RegisterRequest("newuser@example.com", "passw0rd123", "New User");
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.email").value("newuser@example.com"))
            .andExpect(jsonPath("$.data.displayName").value("New User"))
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.data.password_hash").doesNotExist());
    }

    @Test
    @DisplayName("Login with valid credentials → 200, access token in body, refreshToken HttpOnly cookie set")
    void login_success() throws Exception {
        // Register first
        RegisterRequest reg = new RegisterRequest("logintest@example.com", "passw0rd123", "Login Test");
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reg)))
            .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("logintest@example.com", "passw0rd123");
        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(header().string("Set-Cookie", containsString("refreshToken")))
            .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));
    }

    @Test
    @DisplayName("Refresh with valid cookie → 200, new access token, new HttpOnly cookie")
    void refresh_success() throws Exception {
        String refreshCookieValue = registerLoginAndGetRefreshCookie("refresh@example.com");

        mockMvc.perform(post("/api/auth/refresh")
            .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshCookieValue)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(header().string("Set-Cookie", containsString("refreshToken")));
    }

    @Test
    @DisplayName("Logout → 200, cookie cleared (Max-Age=0)")
    void logout_clearsCookie() throws Exception {
        String refreshCookieValue = registerLoginAndGetRefreshCookie("logout@example.com");

        mockMvc.perform(post("/api/auth/logout")
            .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshCookieValue)))
            .andExpect(status().isOk())
            .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    @Test
    @DisplayName("Login with wrong password → 401")
    void login_wrongPassword_401() throws Exception {
        RegisterRequest reg = new RegisterRequest("wrongpw@example.com", "passw0rd123", "Wrong Pw");
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reg)))
            .andExpect(status().isCreated());

        LoginRequest bad = new LoginRequest("wrongpw@example.com", "wrongpassword");
        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(bad)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Register with duplicate email → 409")
    void register_duplicateEmail_409() throws Exception {
        RegisterRequest reg = new RegisterRequest("dup@example.com", "passw0rd123", "Dup");
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reg)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reg)))
            .andExpect(status().isConflict());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String registerLoginAndGetRefreshCookie(String email) throws Exception {
        RegisterRequest reg = new RegisterRequest(email, "passw0rd123", "Test User");
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reg)))
            .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest(email, "passw0rd123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn();

        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookieHeader);
        // Extract the refreshToken value from Set-Cookie header
        for (String part : setCookieHeader.split(";")) {
            part = part.trim();
            if (part.startsWith("refreshToken=")) {
                return part.substring("refreshToken=".length());
            }
        }
        throw new IllegalStateException("refreshToken cookie not found in login response");
    }
}
