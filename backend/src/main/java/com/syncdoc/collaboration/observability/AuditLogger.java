package com.syncdoc.collaboration.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Structured audit logger that emits JSON-compatible log lines for every
 * security-relevant action.  Log aggregation tools (ELK, Loki, CloudWatch)
 * can then query these fields directly.
 */
@Component
public class AuditLogger {

    private static final Logger logger = LoggerFactory.getLogger("AUDIT");

    public enum Action {
        MESSAGE_SENT,
        MESSAGE_DELETED,
        PRESENCE_UPDATED,
        WORKSPACE_ACCESSED,
        SEARCH_EXECUTED,
        THREAD_CREATED,
        RATE_LIMIT_HIT,
        ACCESS_DENIED,
        AUTHENTICATION_FAILURE,
        AUTHENTICATION_SUCCESS
    }

    /**
     * Emits a structured audit event.
     *
     * @param action      the auditable action
     * @param workspaceId workspace context (may be null for auth events)
     * @param userId      acting user
     * @param resourceId  optional resource identifier (message id, thread id, …)
     * @param detail      free-text detail for debugging
     */
    public void log(Action action, String workspaceId, String userId, String resourceId, String detail) {
        // Structured format compatible with Logstash / Loki label parsing
        logger.info(
            "audit={} workspace={} user={} resource={} detail=\"{}\" ts={}",
            action,
            workspaceId != null ? workspaceId : "-",
            userId != null ? userId : "-",
            resourceId != null ? resourceId : "-",
            sanitize(detail),
            Instant.now()
        );
    }

    public void log(Action action, String workspaceId, String userId) {
        log(action, workspaceId, userId, null, null);
    }

    /** Strip newlines and control characters to prevent log injection. */
    private static String sanitize(String input) {
        if (input == null) {
            return "-";
        }
        return input.replaceAll("[\r\n\t]", " ").strip();
    }
}
