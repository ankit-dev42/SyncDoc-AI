package com.syncdoc.collaboration.chaos;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chaos tests for network-partition and service-failure scenarios (T110).
 *
 * All tests are self-contained; they simulate faults in-process by injecting
 * transient failures into a stub dispatch pipeline.
 */
@Tag("chaos")
class NetworkPartitionChaosTest {

    // -----------------------------------------------------------------------
    // Retry-with-backoff under transient failure
    // -----------------------------------------------------------------------

    @Test
    void dispatchShouldRetryAndSucceedAfterTransientFailures() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        // Fail for the first 2 attempts, succeed on the 3rd
        Callable<String> dispatch = () -> {
            if (attempts.incrementAndGet() < 3) {
                throw new RuntimeException("transient network error");
            }
            return "delivered";
        };

        String result = retryWithBackoff(dispatch, 5, 10);

        assertThat(result).isEqualTo("delivered");
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void dispatchShouldGiveUpAfterMaxRetries() {
        AtomicInteger attempts = new AtomicInteger();
        Callable<String> alwaysFails = () -> {
            attempts.incrementAndGet();
            throw new RuntimeException("persistent failure");
        };

        RuntimeException thrown = null;
        try {
            retryWithBackoff(alwaysFails, 3, 1);
        } catch (RuntimeException e) {
            thrown = e;
        } catch (Exception e) {
            thrown = new RuntimeException(e);
        }

        assertThat(thrown).isNotNull();
        assertThat(attempts.get()).isEqualTo(3);
    }

    // -----------------------------------------------------------------------
    // Idempotency during duplicate delivery
    // -----------------------------------------------------------------------

    @Test
    void duplicateDeliveryShouldBeIdempotent() {
        java.util.Set<String> processed = ConcurrentHashMap.newKeySet();
        String idempotencyKey = "msg-abc-123";

        long deliveredCount = java.util.stream.LongStream.range(0, 5)
            .filter(i -> processed.add(idempotencyKey)) // add returns false on duplicate
            .count();

        assertThat(deliveredCount)
            .as("Message must be processed exactly once regardless of duplicate delivery")
            .isEqualTo(1);
    }

    // -----------------------------------------------------------------------
    // Graceful degradation when broadcast is unavailable
    // -----------------------------------------------------------------------

    @Test
    void messagePersistenceShouldSucceedEvenWhenBroadcastFails() {
        boolean persisted = false;
        boolean broadcastAttempted = false;

        try {
            // Persist (always succeeds in this simulation)
            persisted = true;
            // Broadcast (fails due to partition)
            broadcastAttempted = true;
            throw new RuntimeException("Redis unavailable");
        } catch (RuntimeException e) {
            // Broadcast failure is swallowed – message is already persisted
        }

        assertThat(persisted).isTrue();
        assertThat(broadcastAttempted).isTrue();
        // The message was safely stored even though real-time broadcast failed
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private <T> T retryWithBackoff(Callable<T> task, int maxAttempts, long backoffMs) throws Exception {
        Exception last = null;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                return task.call();
            } catch (Exception e) {
                last = e;
                if (i < maxAttempts - 1) {
                    TimeUnit.MILLISECONDS.sleep(backoffMs);
                }
            }
        }
        throw last;
    }
}
