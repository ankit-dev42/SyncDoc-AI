package com.syncdoc.collaboration.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

/**
 * Structured audit logger that emits MDC-enriched log lines for every
 * security-relevant action. In non-local profiles, logstash-logback-encoder
 * serialises all MDC fields as JSON fields automatically.
 *
 * <p>Envelope MDC keys per event: {@code event}, {@code userId}, {@code ip},
 * {@code timestamp}, {@code traceId}.  No sensitive key ({@code accessToken},
 * {@code password}, {@code secret}, {@code rawToken}) may ever be set.
 */
@Component
public class AuditLogger {

    private static final Logger logger = LoggerFactory.getLogger("AUDIT");

    private static final Set<String> FORBIDDEN_KEYS = Set.of("accessToken", "password", "secret", "rawToken");

    public enum Action {
        // Legacy collaboration events (retained for backwards compatibility)
        MESSAGE_SENT,
        MESSAGE_DELETED,
        PRESENCE_UPDATED,
        WORKSPACE_ACCESSED,
        SEARCH_EXECUTED,
        THREAD_CREATED,
        RATE_LIMIT_HIT,
        ACCESS_DENIED,
        AUTHENTICATION_FAILURE,
        AUTHENTICATION_SUCCESS,
        // Phase 4 audit event types (T317)
        AUTH_LOGIN,
        AUTH_LOGOUT,
        AUTH_REFRESH_FAILED,
        BILLING_WEBHOOK_RECEIVED,
        BILLING_SUBSCRIPTION_CHANGED,
        AI_EXTRACTION_COMPLETED,
        AI_EXTRACTION_FAILED,
        WEBHOOK_ACCEPTED,
        WEBHOOK_REJECTED,
        WEBHOOK_DUPLICATE
    }

    // -----------------------------------------------------------------------
    // Public API — deny-list guard
    // -----------------------------------------------------------------------

    /**
     * Sets an MDC key after checking against the forbidden-key deny-list.
     * Package-visible so tests can invoke it directly to verify the guard.
     */
    void putChecked(String key, String value) {
        if (FORBIDDEN_KEYS.contains(key)) {
            throw new IllegalArgumentException(
                "Forbidden MDC key '" + key + "' must never be logged in AuditLogger");
        }
        MDC.put(key, value);
    }

    // -----------------------------------------------------------------------
    // Auth events
    // -----------------------------------------------------------------------

    public void authLogin(String userId, String ip) {
        envelope(Action.AUTH_LOGIN, userId, ip);
        logger.info(Action.AUTH_LOGIN.name());
    }

    public void authLogout(String userId, String ip) {
        envelope(Action.AUTH_LOGOUT, userId, ip);
        logger.info(Action.AUTH_LOGOUT.name());
    }

    public void authRefreshFailed(String userId, String ip, String reason) {
        envelope(Action.AUTH_REFRESH_FAILED, userId, ip);
        MDC.put("reason", sanitize(reason));
        logger.info(Action.AUTH_REFRESH_FAILED.name());
    }

    public void accessDenied(String userId, String requestPath, String ip) {
        envelope(Action.ACCESS_DENIED, userId, ip);
        MDC.put("requestPath", sanitize(requestPath));
        logger.info(Action.ACCESS_DENIED.name());
    }

    // -----------------------------------------------------------------------
    // Billing events
    // -----------------------------------------------------------------------

    public void billingWebhookReceived(String stripeEventId, String type, String ip) {
        envelope(Action.BILLING_WEBHOOK_RECEIVED, "-", ip);
        MDC.put("stripeEventId", sanitize(stripeEventId));
        MDC.put("type", sanitize(type));
        logger.info(Action.BILLING_WEBHOOK_RECEIVED.name());
    }

    public void billingSubscriptionChanged(String maskedCustomerId, String newTier, String ip) {
        envelope(Action.BILLING_SUBSCRIPTION_CHANGED, "-", ip);
        MDC.put("stripeCustomerId", sanitize(maskedCustomerId));
        MDC.put("newTier", sanitize(newTier));
        logger.info(Action.BILLING_SUBSCRIPTION_CHANGED.name());
    }

    // -----------------------------------------------------------------------
    // AI events
    // -----------------------------------------------------------------------

    public void aiExtractionCompleted(String docId, double qualityScore, long durationMs,
                                      String userId, String ip) {
        envelope(Action.AI_EXTRACTION_COMPLETED, userId, ip);
        MDC.put("docId", sanitize(docId));
        MDC.put("qualityScore", String.valueOf(qualityScore));
        MDC.put("durationMs", String.valueOf(durationMs));
        logger.info(Action.AI_EXTRACTION_COMPLETED.name());
    }

    public void aiExtractionFailed(String docId, String reason, String userId, String ip) {
        envelope(Action.AI_EXTRACTION_FAILED, userId, ip);
        MDC.put("docId", sanitize(docId));
        MDC.put("reason", sanitize(reason));
        logger.info(Action.AI_EXTRACTION_FAILED.name());
    }

    // -----------------------------------------------------------------------
    // Webhook events
    // -----------------------------------------------------------------------

    public void webhookAccepted(String payloadHash, String eventType, String ip) {
        envelope(Action.WEBHOOK_ACCEPTED, "-", ip);
        MDC.put("payloadHash", sanitize(payloadHash));
        MDC.put("eventType", sanitize(eventType));
        logger.info(Action.WEBHOOK_ACCEPTED.name());
    }

    public void webhookRejected(String payloadHash, String reason, String ip) {
        envelope(Action.WEBHOOK_REJECTED, "-", ip);
        MDC.put("payloadHash", sanitize(payloadHash));
        MDC.put("reason", sanitize(reason));
        logger.info(Action.WEBHOOK_REJECTED.name());
    }

    public void webhookDuplicate(String payloadHash, String ip) {
        envelope(Action.WEBHOOK_DUPLICATE, "-", ip);
        MDC.put("payloadHash", sanitize(payloadHash));
        logger.info(Action.WEBHOOK_DUPLICATE.name());
    }

    // -----------------------------------------------------------------------
    // Legacy log API (backwards compatibility)
    // -----------------------------------------------------------------------

    public void log(Action action, String workspaceId, String userId, String resourceId, String detail) {
        envelope(action, userId != null ? userId : "-", "-");
        MDC.put("workspaceId", workspaceId != null ? workspaceId : "-");
        MDC.put("resourceId", resourceId != null ? resourceId : "-");
        MDC.put("detail", sanitize(detail));
        logger.info(action.name());
        MDC.remove("workspaceId");
        MDC.remove("resourceId");
        MDC.remove("detail");
        clearEnvelope();
    }

    public void log(Action action, String workspaceId, String userId) {
        log(action, workspaceId, userId, null, null);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void envelope(Action action, String userId, String ip) {
        MDC.put("event", action.name());
        MDC.put("userId", userId != null ? userId : "-");
        MDC.put("ip", ip != null ? ip : "-");
        MDC.put("timestamp", Instant.now().toString());
        String traceId = MDC.get("traceId");
        MDC.put("traceId", traceId != null ? traceId : "-");
    }

    private void clearEnvelope() {
        MDC.remove("event");
        MDC.remove("userId");
        MDC.remove("ip");
        MDC.remove("timestamp");
        // traceId is managed externally (e.g., by a tracing filter) — do not remove it here
    }

    /** Strip newlines and control characters to prevent log injection. */
    static String sanitize(String input) {
        if (input == null) {
            return "-";
        }
        return input.replaceAll("[\r\n\t]", " ").strip();
    }
}

