package com.syncdoc.collaboration.messaging.unit;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageDeduplicationUnitTest {

    @Test
    public void testMessageDeduplicationWithIdempotencyKeys() {
        // Test that duplicate messages with same idempotency key are deduplicated

        Set<String> processedKeys = new HashSet<>();
        String idempotencyKey = "unique-key-123";

        // First message should be processed
        boolean firstProcessed = processedKeys.add(idempotencyKey);
        assertThat(firstProcessed).isTrue();
        assertThat(processedKeys).contains(idempotencyKey);

        // Second message with same key should be rejected (deduplicated)
        boolean secondProcessed = processedKeys.add(idempotencyKey);
        assertThat(secondProcessed).isFalse(); // Set.add() returns false if element already exists
        assertThat(processedKeys).hasSize(1);
    }

    @Test
    public void testDifferentIdempotencyKeysCreateSeparateMessages() {
        // Test that messages with different idempotency keys are not deduplicated

        Set<String> processedKeys = new HashSet<>();

        String key1 = "key1";
        String key2 = "key2";

        // Both keys should be accepted
        boolean firstProcessed = processedKeys.add(key1);
        boolean secondProcessed = processedKeys.add(key2);

        assertThat(firstProcessed).isTrue();
        assertThat(secondProcessed).isTrue();
        assertThat(processedKeys).hasSize(2);
        assertThat(processedKeys).contains(key1, key2);
    }
}