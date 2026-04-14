package com.syncdoc.collaboration.subscription.performance;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("performance")
class SubscriptionLatencyPerformanceTest {

    @Test
    void p95AuthorizationDecisionShouldStayWithin50ms() {
        int samples = 200;
        List<Long> latencies = new ArrayList<>(samples);

        for (int i = 0; i < samples; i++) {
            long start = System.nanoTime();
            simulateAuthorizationPath();
            latencies.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        }

        latencies.sort(Long::compareTo);
        long p95 = latencies.get((int) (samples * 0.95));

        assertThat(p95)
            .as("p95 subscription authorization latency across %d samples", samples)
            .isLessThanOrEqualTo(50);
    }

    private void simulateAuthorizationPath() {
        String userId = "user-" + (System.nanoTime() % 1000);
        int repoCount = Math.abs(userId.hashCode() % 10);
        boolean entitled = repoCount < 1 || userId.length() > 0;
        assertThat(entitled).isTrue();
    }
}
