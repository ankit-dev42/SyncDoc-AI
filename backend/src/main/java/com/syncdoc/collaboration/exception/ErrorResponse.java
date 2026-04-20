package com.syncdoc.collaboration.exception;

import java.time.Instant;
import java.util.List;

/**
 * Standardised error response returned by all exception handlers.
 *
 * <p>Every 4xx and 5xx response from this application MUST use this record,
 * ensuring that API consumers can parse errors with a single schema.
 *
 * @param error     machine-readable error code (e.g. {@code "VALIDATION_FAILED"})
 * @param message   human-readable error description
 * @param details   per-field validation errors; empty for non-validation errors
 * @param timestamp the instant the error occurred
 * @param path      the request URI that triggered the error
 */
public record ErrorResponse(
    String error,
    String message,
    List<FieldError> details,
    Instant timestamp,
    String path
) {

    /**
     * Per-field validation error detail.
     *
     * @param field          the field that failed validation
     * @param rejectedValue  the value that was rejected
     * @param message        the validation message
     */
    public record FieldError(String field, Object rejectedValue, String message) {
    }
}
