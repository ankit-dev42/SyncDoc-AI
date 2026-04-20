package com.syncdoc.collaboration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncdoc.collaboration.common.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-IP rate limiter.
 * <ul>
 *   <li>{@code /api/auth/**} — 10 requests per minute (brute-force protection)</li>
 *   <li>{@code /api/v1/**}   — 240 requests per minute</li>
 * </ul>
 * IP resolution honours {@code X-Forwarded-For} (first entry) for reverse-proxy deployments.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final int authLimit;
    private final int apiLimit;

    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper,
                           @Value("${rate-limit.auth-per-minute:10}") int authLimit,
                           @Value("${rate-limit.api-per-minute:240}") int apiLimit) {
        this.objectMapper = objectMapper;
        this.authLimit = authLimit;
        this.apiLimit = apiLimit;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String uri = request.getRequestURI();
        int limit;
        if (uri.startsWith("/api/auth/")) {
            limit = authLimit;
        } else if (uri.startsWith("/api/v1/")) {
            limit = apiLimit;
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        String bucket = Instant.now().truncatedTo(ChronoUnit.MINUTES).toString();
        String clientIp = resolveClientIp(request);
        String counterKey = clientIp + ":" + uri.split("/")[2] + ":" + bucket;
        int current = counters.computeIfAbsent(counterKey, ignored -> new AtomicInteger()).incrementAndGet();

        if (current > limit) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error("Rate limit exceeded. Please retry in one minute.")
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

