package com.syncdoc.collaboration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncdoc.collaboration.auth.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Replaces {@code HeaderAuthenticationFilter}. Validates the {@code Authorization: Bearer} token
 * on every request and populates the {@link org.springframework.security.core.context.SecurityContext}.
 *
 * <p>On missing header: passes through (anonymous — downstream security rules handle 401).
 * On invalid/tampered JWT: writes JSON 401 immediately.
 * On expired JWT: writes JSON 401 with {@code error: "TOKEN_EXPIRED"}.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, ObjectMapper objectMapper) {
        this.jwtTokenService = jwtTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            Claims claims = jwtTokenService.validateAccessToken(token);
            String userId = jwtTokenService.extractUserId(claims);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED",
                "Access token has expired. Please refresh your session.", request.getRequestURI());
        } catch (JwtException e) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN",
                "JWT token is invalid or has been tampered with.", request.getRequestURI());
        }
    }

    private void writeErrorResponse(
        HttpServletResponse response,
        HttpStatus status,
        String error,
        String message,
        String path
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = Map.of(
            "error", error,
            "message", message,
            "timestamp", Instant.now().toString(),
            "path", path
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
