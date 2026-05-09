package com.syncdoc.collaboration.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.syncdoc.collaboration.observability.AuditLogger;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Single unified exception handler — all 4xx/5xx responses use {@link ErrorResponse}.
 * {@code @Order(1)} ensures this advice takes precedence over any residual advice beans.
 */
@Order(1)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuditLogger auditLogger;

    public GlobalExceptionHandler(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    // -----------------------------------------------------------------------
    // 400 — Validation
    // -----------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        MethodArgumentNotValidException ex, HttpServletRequest request
    ) {
        List<ErrorResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getRejectedValue(), fe.getDefaultMessage()))
            .collect(Collectors.toList());

        logger.debug("Validation failure on {}: {} field(s)", request.getRequestURI(), details.size());
        return ResponseEntity.badRequest().body(new ErrorResponse(
            "VALIDATION_FAILED", "Request validation failed", details, Instant.now(), request.getRequestURI()
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
        ConstraintViolationException ex, HttpServletRequest request
    ) {
        List<ErrorResponse.FieldError> details = ex.getConstraintViolations().stream()
            .map(cv -> new ErrorResponse.FieldError(
                cv.getPropertyPath().toString(), cv.getInvalidValue(), cv.getMessage()))
            .collect(Collectors.toList());

        return ResponseEntity.badRequest().body(new ErrorResponse(
            "CONSTRAINT_VIOLATION", "Constraint validation failed", details, Instant.now(), request.getRequestURI()
        ));
    }

    // -----------------------------------------------------------------------
    // Business validation — merged from BusinessValidationExceptionHandler
    // -----------------------------------------------------------------------

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessValidation(
        BusinessValidationException ex, HttpServletRequest request
    ) {
        logger.warn("Business validation failure on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.valueOf(ex.getStatusCode())).body(new ErrorResponse(
            ex.getErrorCode(), ex.getMessage(), List.of(), Instant.now(), request.getRequestURI()
        ));
    }

    // -----------------------------------------------------------------------
    // 401/403 — Auth
    // -----------------------------------------------------------------------

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
        AccessDeniedException ex, HttpServletRequest request
    ) {
        logger.warn("Access denied to {} from {}", request.getRequestURI(), request.getRemoteAddr());
        String userId = "-";
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            userId = authentication.getName();
        }
        auditLogger.accessDenied(userId, request.getRequestURI(), request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(
            "ACCESS_DENIED", "You do not have permission to access this resource",
            List.of(), Instant.now(), request.getRequestURI()
        ));
    }

    // -----------------------------------------------------------------------
    // 404 — Not found
    // -----------------------------------------------------------------------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
        ResourceNotFoundException ex, HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
            "NOT_FOUND", ex.getMessage(), List.of(), Instant.now(), request.getRequestURI()
        ));
    }

    // -----------------------------------------------------------------------
    // Spring ResponseStatusException (used by services)
    // -----------------------------------------------------------------------

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
        ResponseStatusException ex, HttpServletRequest request
    ) {
        return ResponseEntity.status(ex.getStatusCode()).body(new ErrorResponse(
            "REQUEST_ERROR", ex.getReason() != null ? ex.getReason() : ex.getMessage(),
            List.of(), Instant.now(), request.getRequestURI()
        ));
    }

    // -----------------------------------------------------------------------
    // Multi-tenancy violations
    // -----------------------------------------------------------------------

    @ExceptionHandler(MultiTenancyViolationException.class)
    public ResponseEntity<ErrorResponse> handleMultiTenancy(
        MultiTenancyViolationException ex, HttpServletRequest request
    ) {
        logger.warn("Multi-tenancy violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(
            "TENANCY_VIOLATION", "You do not have access to this workspace",
            List.of(), Instant.now(), request.getRequestURI()
        ));
    }

    // -----------------------------------------------------------------------
    // 500 — Fallback
    // -----------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
        Exception ex, HttpServletRequest request
    ) {
        logger.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.internalServerError().body(new ErrorResponse(
            "INTERNAL_ERROR", "An unexpected error occurred. Please try again later.",
            List.of(), Instant.now(), request.getRequestURI()
        ));
    }
}

