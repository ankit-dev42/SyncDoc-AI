package com.syncdoc.collaboration.exception;

public class MessageOrderingError extends RuntimeException {
    public MessageOrderingError(String message) {
        super(message);
    }
}