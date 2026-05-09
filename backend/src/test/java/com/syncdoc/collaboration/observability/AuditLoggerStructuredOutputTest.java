package com.syncdoc.collaboration.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link AuditLogger} structured MDC output (T307).
 *
 * <p>Verifies that every event method:
 * <ul>
 *   <li>Sets all 5 required envelope keys in MDC before the log call</li>
 *   <li>Never adds any forbidden sensitive key to MDC</li>
 *   <li>{@code sanitize()} strips newlines and carriage returns from detail strings</li>
 * </ul>
 *
 * <p>These tests MUST fail before T317 implements the new event methods in
 * {@code AuditLogger}.
 */
@ExtendWith(MockitoExtension.class)
class AuditLoggerStructuredOutputTest {

    private static final Set<String> FORBIDDEN_KEYS = Set.of("accessToken", "password", "secret", "rawToken");
    private static final Set<String> REQUIRED_ENVELOPE_KEYS = Set.of("event", "userId", "ip", "timestamp", "traceId");

    private AuditLogger auditLogger;

    @BeforeEach
    void setUp() {
        auditLogger = new AuditLogger();
        MDC.clear();
        MDC.put("traceId", "trace-test-123");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    // -----------------------------------------------------------------------
    // Envelope key assertions — helper
    // -----------------------------------------------------------------------

    private void assertEnvelopeKeys() {
        for (String key : REQUIRED_ENVELOPE_KEYS) {
            assertThat(MDC.get(key))
                .as("MDC key '%s' must be non-null after event method call", key)
                .isNotNull();
        }
    }

    private void assertNoForbiddenKeys() {
        for (String key : FORBIDDEN_KEYS) {
            assertThat(MDC.get(key))
                .as("Forbidden MDC key '%s' must never be set", key)
                .isNull();
        }
    }

    // -----------------------------------------------------------------------
    // AUTH_LOGIN
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("authLogin: sets all envelope keys, no forbidden keys")
    void authLogin_setsEnvelopeKeys() {
        auditLogger.authLogin("user-123", "192.168.1.1");
        assertEnvelopeKeys();
        assertNoForbiddenKeys();
    }

    // -----------------------------------------------------------------------
    // AUTH_LOGOUT
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("authLogout: sets all envelope keys, no forbidden keys")
    void authLogout_setsEnvelopeKeys() {
        auditLogger.authLogout("user-123", "192.168.1.1");
        assertEnvelopeKeys();
        assertNoForbiddenKeys();
    }

    // -----------------------------------------------------------------------
    // AUTH_REFRESH_FAILED
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("authRefreshFailed: sets all envelope keys, no forbidden keys")
    void authRefreshFailed_setsEnvelopeKeys() {
        auditLogger.authRefreshFailed("user-123", "192.168.1.1", "token_expired");
        assertEnvelopeKeys();
        assertNoForbiddenKeys();
    }

    // -----------------------------------------------------------------------
    // ACCESS_DENIED
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("accessDenied: sets all envelope keys, no forbidden keys")
    void accessDenied_setsEnvelopeKeys() {
        auditLogger.accessDenied("user-123", "/api/v1/admin", "192.168.1.1");
        assertEnvelopeKeys();
        assertNoForbiddenKeys();
    }

    // -----------------------------------------------------------------------
    // BILLING_WEBHOOK_RECEIVED
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("billingWebhookReceived: sets all envelope keys, no forbidden keys")
    void billingWebhookReceived_setsEnvelopeKeys() {
        auditLogger.billingWebhookReceived("evt_123", "customer.subscription.updated", "192.168.1.1");
        assertEnvelopeKeys();
        assertNoForbiddenKeys();
    }

    // -----------------------------------------------------------------------
    // BILLING_SUBSCRIPTION_CHANGED
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("billingSubscriptionChanged: sets all envelope keys, no forbidden keys")
    void billingSubscriptionChanged_setsEnvelopeKeys() {
        auditLogger.billingSubscriptionChanged("cus_****1234", "ENTERPRISE", "192.168.1.1");
        assertEnvelopeKeys();
        assertNoForbiddenKeys();
    }

    // -----------------------------------------------------------------------
    // AI_EXTRACTION_COMPLETED
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("aiExtractionCompleted: sets all envelope keys, no forbidden keys")
    void aiExtractionCompleted_setsEnvelopeKeys() {
        auditLogger.aiExtractionCompleted("doc-abc", 0.95, 350L, "user-123", "192.168.1.1");
        assertEnvelopeKeys();
        assertNoForbiddenKeys();
    }

    // -----------------------------------------------------------------------
    // AI_EXTRACTION_FAILED
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("aiExtractionFailed: sets all envelope keys, no forbidden keys")
    void aiExtractionFailed_setsEnvelopeKeys() {
        auditLogger.aiExtractionFailed("doc-abc", "openai_timeout", "user-123", "192.168.1.1");
        assertEnvelopeKeys();
        assertNoForbiddenKeys();
    }

    // -----------------------------------------------------------------------
    // WEBHOOK_ACCEPTED
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("webhookAccepted: sets all envelope keys, no forbidden keys")
    void webhookAccepted_setsEnvelopeKeys() {
        auditLogger.webhookAccepted("abc123hash", "push", "192.168.1.1");
        assertEnvelopeKeys();
        assertNoForbiddenKeys();
    }

    // -----------------------------------------------------------------------
    // WEBHOOK_REJECTED
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("webhookRejected: sets all envelope keys, no forbidden keys")
    void webhookRejected_setsEnvelopeKeys() {
        auditLogger.webhookRejected("abc123hash", "signature_invalid", "192.168.1.1");
        assertEnvelopeKeys();
        assertNoForbiddenKeys();
    }

    // -----------------------------------------------------------------------
    // WEBHOOK_DUPLICATE
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("webhookDuplicate: sets all envelope keys, no forbidden keys")
    void webhookDuplicate_setsEnvelopeKeys() {
        auditLogger.webhookDuplicate("abc123hash", "192.168.1.1");
        assertEnvelopeKeys();
        assertNoForbiddenKeys();
    }

    // -----------------------------------------------------------------------
    // Sanitize — strips newlines and carriage returns
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("sanitize strips newline from detail string")
    void sanitize_stripsNewlines() {
        // authRefreshFailed uses the reason field which goes through sanitize
        auditLogger.authRefreshFailed("user-123", "10.0.0.1", "bad\ntoken\r\nreason");
        String reason = MDC.get("reason");
        assertThat(reason).doesNotContain("\n").doesNotContain("\r");
    }

    // -----------------------------------------------------------------------
    // Forbidden key injection protection
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("putForbiddenKey throws IllegalArgumentException for 'accessToken'")
    void forbiddenKey_accessToken_throws() {
        assertThatThrownBy(() -> auditLogger.putChecked("accessToken", "some-value"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("accessToken");
    }

    @Test
    @DisplayName("putForbiddenKey throws IllegalArgumentException for 'password'")
    void forbiddenKey_password_throws() {
        assertThatThrownBy(() -> auditLogger.putChecked("password", "secret123"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
