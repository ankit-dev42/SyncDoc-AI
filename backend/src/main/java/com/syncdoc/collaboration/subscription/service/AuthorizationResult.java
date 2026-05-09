package com.syncdoc.collaboration.subscription.service;

/**
 * Result of an authorization check performed by {@link SyncAuthorizationService#authorize(String, int)}.
 */
public enum AuthorizationResult {
    ALLOW,
    DENY
}
