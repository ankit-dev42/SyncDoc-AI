package com.syncdoc.collaboration.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight in-process metrics collector.
 *
 * In production this can be swapped for Micrometer by injecting a {@code MeterRegistry};
 * keeping it self-contained here avoids adding a required Actuator/Prometheus dependency
 * while still satisfying T100 (performance monitoring).
 */
@Component
public class MetricsService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);

    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> latencyTotals = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> latencyCounts = new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // Counters
    // -----------------------------------------------------------------------

    public void increment(String metric) {
        counters.computeIfAbsent(metric, k -> new AtomicLong()).incrementAndGet();
    }

    public long getCount(String metric) {
        AtomicLong counter = counters.get(metric);
        return counter == null ? 0L : counter.get();
    }

    // -----------------------------------------------------------------------
    // Latency (average)
    // -----------------------------------------------------------------------

    public void recordLatency(String operation, long durationMs) {
        latencyTotals.computeIfAbsent(operation, k -> new AtomicLong()).addAndGet(durationMs);
        latencyCounts.computeIfAbsent(operation, k -> new AtomicLong()).incrementAndGet();

        if (durationMs > 200) {
            logger.warn("Slow operation detected [{}]: {}ms (threshold 200ms)", operation, durationMs);
        }
    }

    public double getAverageLatencyMs(String operation) {
        AtomicLong total = latencyTotals.get(operation);
        AtomicLong count = latencyCounts.get(operation);
        if (total == null || count == null || count.get() == 0) {
            return 0.0;
        }
        return (double) total.get() / count.get();
    }

    /** Convenience: time a runnable and record its latency. */
    public void timed(String operation, Runnable task) {
        long start = System.currentTimeMillis();
        try {
            task.run();
        } finally {
            recordLatency(operation, System.currentTimeMillis() - start);
        }
    }

    // -----------------------------------------------------------------------
    // Snapshot for health endpoint
    // -----------------------------------------------------------------------

    public Map<String, Object> snapshot() {
        Map<String, Object> snap = new ConcurrentHashMap<>();
        counters.forEach((k, v) -> snap.put("counter." + k, v.get()));
        latencyCounts.forEach((k, v) -> {
            AtomicLong total = latencyTotals.get(k);
            if (total != null && v.get() > 0) {
                snap.put("latency.avg." + k + "_ms", (double) total.get() / v.get());
            }
        });
        return snap;
    }
}
