package com.syncdoc.collaboration.exception;

public class MultiTenancyViolationException extends RuntimeException {
    public MultiTenancyViolationException(String message) {
        super(message);
    }
}