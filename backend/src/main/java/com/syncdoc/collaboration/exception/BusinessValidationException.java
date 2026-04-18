package com.syncdoc.collaboration.exception;

public class BusinessValidationException extends RuntimeException {

    private final int statusCode;
    private final String errorCode;

    public BusinessValidationException(int statusCode, String errorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}