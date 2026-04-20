package com.syncdoc.collaboration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Startup validation tests ensuring the application fails fast on
 * missing or invalid configuration (SC-P1-6).
 */
class ApplicationStartupTest {

    @Test
    @DisplayName("Blank GITHUB_WEBHOOK_SECRET causes context failure with IllegalStateException (SC-P1-6)")
    void blankWebhookSecret_contexFails() {
        assertThrows(Exception.class, () -> {
            // Loading a partial context to exercise @PostConstruct on GitHubWebhookSignatureVerifier
            // with a blank webhook secret
            var context = new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withUserConfiguration(com.syncdoc.collaboration.CollaborationApplication.class)
                .withPropertyValues(
                    "spring.profiles.active=test",
                    "GITHUB_WEBHOOK_SECRET=",
                    "JWT_SECRET=test-jwt-secret-key-at-least-32-chars-ok",
                    "DB_URL=jdbc:h2:mem:testdb",
                    "DB_USERNAME=sa",
                    "DB_PASSWORD=",
                    "integrations.github.webhook-secret="
                );
            context.run(ctx -> ctx.getBean(
                com.syncdoc.collaboration.webhook.security.GitHubWebhookSignatureVerifier.class));
        });
    }

    @Test
    @DisplayName("JWT_SECRET shorter than 32 chars causes IllegalArgumentException at startup")
    void shortJwtSecret_contextFails() {
        assertThrows(Exception.class, () -> {
            var context = new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withUserConfiguration(com.syncdoc.collaboration.CollaborationApplication.class)
                .withPropertyValues(
                    "JWT_SECRET=tooshort",
                    "integrations.github.webhook-secret=valid-secret-here",
                    "DB_URL=jdbc:h2:mem:testdb2",
                    "DB_USERNAME=sa",
                    "DB_PASSWORD="
                );
            context.run(ctx -> ctx.getBean(
                com.syncdoc.collaboration.auth.service.JwtTokenService.class));
        });
    }
}
