package com.syncdoc.collaboration.messaging.unit;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadNotificationUnitTest {

    @Test
    void shouldNotifyExistingThreadParticipantsExceptActor() {
        String actorId = "user-c";
        Set<String> participants = new LinkedHashSet<>();
        participants.add("user-a");
        participants.add("user-b");
        participants.add("user-c");

        Set<String> recipients = participants.stream()
            .filter(userId -> !userId.equals(actorId))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        assertThat(recipients).containsExactly("user-a", "user-b");
        assertThat(recipients).doesNotContain(actorId);
    }
}
