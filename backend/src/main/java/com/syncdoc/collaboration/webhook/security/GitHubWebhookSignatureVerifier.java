package com.syncdoc.collaboration.webhook.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Verifies GitHub webhook HMAC-SHA256 signatures.
 * Fails fast at startup if {@code GITHUB_WEBHOOK_SECRET} is blank.
 */
@Component
public class GitHubWebhookSignatureVerifier {

    private static final String PREFIX = "sha256=";
    private static final String HMAC_SHA256 = "HmacSHA256";

    @Value("${integrations.github.webhook-secret:}")
    private String webhookSecret;

    /**
     * Validates that the GitHub webhook secret is configured.
     * Throws {@link IllegalStateException} at startup if the secret is blank.
     */
    @PostConstruct
    public void validateConfiguration() {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException(
                "GITHUB_WEBHOOK_SECRET must not be blank. " +
                "Set it via the GITHUB_WEBHOOK_SECRET environment variable.");
        }
    }

    public boolean isValid(String signatureHeader, String payload, String secret) {
        if (signatureHeader == null || !signatureHeader.startsWith(PREFIX) || secret == null || secret.isBlank()) {
            return false;
        }

        String expected = PREFIX + computeHexDigest(payload, secret);
        return constantTimeEquals(expected, signatureHeader);
    }

    private String computeHexDigest(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(keySpec);
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute webhook signature", ex);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}

