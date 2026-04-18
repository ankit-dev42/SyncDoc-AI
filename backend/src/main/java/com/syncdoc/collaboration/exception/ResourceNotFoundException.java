package com.syncdoc.collaboration.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resource, String id) {
        return new ResourceNotFoundException(resource + " not found: " + id);
    }
}
