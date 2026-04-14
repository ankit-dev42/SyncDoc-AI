package com.syncdoc.collaboration.presence.unit;

import com.syncdoc.collaboration.presence.model.Presence;
import com.syncdoc.collaboration.presence.service.PresenceService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class PresenceAutoAwayUnitTest {

    @Test
    void shouldMarkUserAwayAfterInactivityThreshold() {
        Instant now = Instant.now();
        Instant lastSeen = now.minus(Duration.ofMinutes(6));

        Presence.Status derived = PresenceService.deriveStatus(lastSeen, null, now, Duration.ofMinutes(5));

        assertThat(derived).isEqualTo(Presence.Status.AWAY);
    }

    @Test
    void shouldKeepManualStatusWhenSet() {
        Instant now = Instant.now();
        Instant lastSeen = now.minus(Duration.ofHours(1));

        Presence.Status derived = PresenceService.deriveStatus(lastSeen, Presence.Status.OFFLINE, now, Duration.ofMinutes(5));

        assertThat(derived).isEqualTo(Presence.Status.OFFLINE);
    }
}
