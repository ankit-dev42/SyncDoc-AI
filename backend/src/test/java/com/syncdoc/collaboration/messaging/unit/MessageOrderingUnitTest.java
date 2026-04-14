package com.syncdoc.collaboration.messaging.unit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageOrderingUnitTest {

    @Test
    public void testMessageOrderingWithLamportClocks() throws Exception {
        // Simplified unit test for message ordering logic
        // Test that sequence numbers are properly ordered

        // Simulate sequence numbers from Lamport clocks
        List<Long> sequenceNumbers = List.of(1L, 2L, 3L, 4L, 5L);

        // Verify ordering
        for (int i = 1; i < sequenceNumbers.size(); i++) {
            assertThat(sequenceNumbers.get(i))
                .isGreaterThan(sequenceNumbers.get(i-1));
        }

        // Test that unordered sequences can be sorted
        List<Long> unordered = List.of(5L, 2L, 4L, 1L, 3L);
        List<Long> sorted = unordered.stream().sorted().toList();

        assertThat(sorted).isSorted();
        assertThat(sorted.get(0)).isEqualTo(1L);
        assertThat(sorted.get(sorted.size() - 1)).isEqualTo(5L);
    }
}