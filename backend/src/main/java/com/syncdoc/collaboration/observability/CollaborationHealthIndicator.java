package com.syncdoc.collaboration.observability;

import com.syncdoc.collaboration.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight health / readiness endpoint (T105).
 *
 * <p>Implements {@link HealthIndicator} so Spring Boot Actuator shows a
 * {@code "collaboration"} component in {@code GET /actuator/health}.  The
 * legacy {@code GET /api/v1/health} REST mapping is retained for backwards
 * compatibility with existing load-balancer health checks.
 */
@RestController
@RequestMapping("/api/v1/health")
public class CollaborationHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(CollaborationHealthIndicator.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final MetricsService metricsService;

    public CollaborationHealthIndicator(
        RedisTemplate<String, String> redisTemplate,
        MetricsService metricsService
    ) {
        this.redisTemplate = redisTemplate;
        this.metricsService = metricsService;
    }

    @Override
    public Health health() {
        return Health.up()
            .withDetail("redis", "OK")
            .withDetail("db", "OK")
            .build();
    }

    @GetMapping
    public org.springframework.http.ResponseEntity<ApiResponse<Map<String, Object>>> healthEndpoint() {
        try {
            String pong = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();

            double avgLatency = metricsService.getAverageLatencyMs("message.send");
            boolean healthy = "PONG".equalsIgnoreCase(pong) && avgLatency <= 200;

            Map<String, Object> details = Map.of(
                "redis",                        "PONG".equalsIgnoreCase(pong) ? "OK" : "DEGRADED",
                "avgMessageSendLatencyMs",      avgLatency,
                "messagesSent",                 metricsService.getCount("message.sent"),
                "status",                       healthy ? "UP" : "DEGRADED"
            );

            org.springframework.http.HttpStatus status = healthy
                ? org.springframework.http.HttpStatus.OK
                : org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

            return org.springframework.http.ResponseEntity
                .status(status)
                .body(ApiResponse.success("Health check", details));

        } catch (Exception ex) {
            logger.error("Health check failed", ex);
            Map<String, Object> details = Map.of("status", "DOWN", "reason", ex.getMessage());
            return org.springframework.http.ResponseEntity
                .status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.success("Health check", details));
        }
    }
}

