package com.syncdoc.collaboration.messaging.integration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class UnreadCountIntegrationTest {

    @Test
    void shouldCalculateUnreadCountFromLastReadSequence() {
        List<Long> sequenceNumbers = List.of(101L, 102L, 103L, 104L, 105L);
        long lastReadSequence = 102L;

        long unreadCount = sequenceNumbers.stream()
            .filter(seq -> seq > lastReadSequence)
            .count();

        assertThat(unreadCount).isEqualTo(3);
    }
}
