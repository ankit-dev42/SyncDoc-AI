package com.syncdoc.collaboration.auth.controller;

import com.syncdoc.collaboration.auth.dto.LoginRequest;
import com.syncdoc.collaboration.auth.dto.RegisterRequest;
import com.syncdoc.collaboration.auth.dto.TokenResponse;
import com.syncdoc.collaboration.auth.dto.UserResponse;
import com.syncdoc.collaboration.auth.service.AuthService;
import com.syncdoc.collaboration.common.dto.ApiResponse;
import com.syncdoc.collaboration.observability.AuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth endpoints — all routes are permitAll (no JWT required).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuditLogger auditLogger;

    public AuthController(AuthService authService, AuditLogger auditLogger) {
        this.authService = authService;
        this.auditLogger = auditLogger;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
        @Valid @RequestBody RegisterRequest request
    ) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("User registered successfully", user));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        TokenResponse token = authService.login(request, httpRequest, httpResponse);
        auditLogger.authLogin("-", httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Login successful", token));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
        @CookieValue(name = AuthService.REFRESH_COOKIE_NAME, required = false) String refreshToken,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            auditLogger.authRefreshFailed("-", httpRequest.getRemoteAddr(), "missing_refresh_cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Refresh token cookie is missing"));
        }
        try {
            TokenResponse token = authService.refresh(refreshToken, httpRequest, httpResponse);
            return ResponseEntity.ok(ApiResponse.success("Token refreshed", token));
        } catch (Exception ex) {
            auditLogger.authRefreshFailed("-", httpRequest.getRemoteAddr(), ex.getClass().getSimpleName());
            throw ex;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
        @CookieValue(name = AuthService.REFRESH_COOKIE_NAME, required = false) String refreshToken,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken, httpResponse);
        }
        auditLogger.authLogout("-", httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }
}
