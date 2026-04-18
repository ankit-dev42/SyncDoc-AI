package com.syncdoc.collaboration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncdoc.collaboration.common.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int REQUESTS_PER_MINUTE = 240;

    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String bucket = Instant.now().truncatedTo(ChronoUnit.MINUTES).toString();
        String clientId = request.getRemoteAddr() + ":" + bucket;
        int current = counters.computeIfAbsent(clientId, ignored -> new AtomicInteger()).incrementAndGet();

        if (current > REQUESTS_PER_MINUTE) {
            response.setStatus(429); // 429 Too Many Requests
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error("Rate limit exceeded. Please retry in one minute.")
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
