package com.syncdoc.collaboration.exception;

import com.syncdoc.collaboration.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class BusinessValidationExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(BusinessValidationExceptionHandler.class);

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleBusinessValidation(
        BusinessValidationException ex,
        HttpServletRequest request
    ) {
        logger.warn("Business validation failure on {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("errorCode", ex.getErrorCode());
        payload.put("path", request.getRequestURI());

        HttpStatus status = HttpStatus.resolve(ex.getStatusCode());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }

        ApiResponse<Map<String, String>> response = ApiResponse.error(ex.getMessage());
        response.setData(payload);

        return ResponseEntity.status(status)
            .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        Map<String, String> payload = new LinkedHashMap<>();
        FieldError fieldError = ex.getBindingResult().getFieldError();
        if (fieldError != null) {
            payload.put("field", fieldError.getField());
            payload.put("error", fieldError.getDefaultMessage());
        }
        payload.put("path", request.getRequestURI());

        ApiResponse<Map<String, String>> response = ApiResponse.error("Validation failed");
        response.setData(payload);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolation(
        ConstraintViolationException ex,
        HttpServletRequest request
    ) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("error", ex.getMessage());
        payload.put("path", request.getRequestURI());

        ApiResponse<Map<String, String>> response = ApiResponse.error("Validation failed");
        response.setData(payload);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}