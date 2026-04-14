package com.syncdoc.collaboration.presence.unit;

import com.syncdoc.collaboration.presence.model.Presence;
import com.syncdoc.collaboration.presence.service.PresenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the idle / away-detection logic in {@link PresenceService#deriveStatus}.
 */
public class IdleDetectionUnitTest {

    private static final Duration THRESHOLD = Duration.ofMinutes(5);

    @Test
    void activeUserShouldBeOnline() {
        Instant now = Instant.now();
        Instant lastSeen = now.minus(Duration.ofSeconds(30));

        assertThat(PresenceService.deriveStatus(lastSeen, null, now, THRESHOLD))
            .isEqualTo(Presence.Status.ONLINE);
    }

    @ParameterizedTest
    @ValueSource(ints = {6, 10, 60, 300})
    void idleUserShouldBecomeAway(int idleMinutes) {
        Instant now = Instant.now();
        Instant lastSeen = now.minus(Duration.ofMinutes(idleMinutes));

        assertThat(PresenceService.deriveStatus(lastSeen, null, now, THRESHOLD))
            .isEqualTo(Presence.Status.AWAY);
    }

    @Test
    void manualDndStatusShouldPersistRegardlessOfActivity() {
        // DND is modelled as OFFLINE manual status in the current enum
        Instant now = Instant.now();
        Instant lastSeen = now.minusSeconds(5); // very recent

        assertThat(PresenceService.deriveStatus(lastSeen, Presence.Status.OFFLINE, now, THRESHOLD))
            .isEqualTo(Presence.Status.OFFLINE);
    }

    @Test
    void idleDetectionShouldRespectCustomThreshold() {
        Duration shortThreshold = Duration.ofMinutes(1);
        Instant now = Instant.now();
        Instant lastSeen = now.minus(Duration.ofMinutes(2));

        assertThat(PresenceService.deriveStatus(lastSeen, null, now, shortThreshold))
            .isEqualTo(Presence.Status.AWAY);
    }

    @Test
    void userActiveWithinCustomThresholdShouldBeOnline() {
        Duration longThreshold = Duration.ofMinutes(30);
        Instant now = Instant.now();
        Instant lastSeen = now.minus(Duration.ofMinutes(10));

        assertThat(PresenceService.deriveStatus(lastSeen, null, now, longThreshold))
            .isEqualTo(Presence.Status.ONLINE);
    }
}

