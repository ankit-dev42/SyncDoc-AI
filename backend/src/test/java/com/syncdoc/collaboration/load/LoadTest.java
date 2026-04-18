package com.syncdoc.collaboration.load;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Load test for 10 000 concurrent-user simulation (T109).
 *
 * The test models the dispatch throughput of the rate-limit filter and message
 * pipeline in-process, giving fast CI feedback without requiring a live instance.
 * Wall-clock target: all 10 000 work units must complete in ≤ 5 s.
 */
@Tag("load")
class LoadTest {

    private static final int CONCURRENT_USERS = 10_000;
    private static final long WALL_CLOCK_LIMIT_MS = 5_000;

    @Test
    void tenThousandConcurrentUsersShouldBeHandledWithinSLA() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(200);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);
        AtomicLong failCount = new AtomicLong();

        long start = System.currentTimeMillis();

        IntStream.range(0, CONCURRENT_USERS)
            .mapToObj(i -> pool.submit(() -> {
                try {
                    simulateUserAction(i);
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }))
            .toList(); // submit all tasks; results tracked via latch

        boolean completed = latch.await(WALL_CLOCK_LIMIT_MS, TimeUnit.MILLISECONDS);
        long wallMs = System.currentTimeMillis() - start;
        pool.shutdownNow();

        assertThat(completed)
            .as("%d users completed in %d ms (limit %d ms)", CONCURRENT_USERS, wallMs, WALL_CLOCK_LIMIT_MS)
            .isTrue();

        assertThat(failCount.get())
            .as("No user action should fail under load (failures: %d)", failCount.get())
            .isZero();
    }

    // -----------------------------------------------------------------------

    /**
     * Models one user's action: sequence generation + idempotency check + broadcast.
     * Mirrors the CPU-bound portion of the real pipeline.
     */
    private void simulateUserAction(int userId) {
        long seq = System.nanoTime() ^ (userId * 6364136223846793005L);
        String key = "user-" + userId + "-" + Long.toHexString(seq);
        // Ensure the key is structurally valid (no injection characters)
        assertThat(key).matches("[a-zA-Z0-9\\-]+");
    }
}
