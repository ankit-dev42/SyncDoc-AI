package com.syncdoc.collaboration.search.performance;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class SearchPerformanceTest {

    @Test
    void shouldFilterWithinPerformanceBudgetForLargeInMemorySet() {
        List<String> corpus = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            corpus.add("message-" + i + " baseline text");
        }
        corpus.set(123, "database architecture proposal");
        corpus.set(4567, "database indexing tradeoffs");

        long start = System.currentTimeMillis();
        List<String> results = corpus.stream()
            .filter(m -> m.toLowerCase(Locale.ROOT).contains("database"))
            .toList();
        long elapsed = System.currentTimeMillis() - start;

        assertThat(results).hasSize(2);
        assertThat(elapsed).isLessThan(500);
    }
}
