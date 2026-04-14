package com.syncdoc.collaboration.presence.integration;

import com.syncdoc.collaboration.messaging.service.MessageService;
import com.syncdoc.collaboration.messaging.websocket.RedisWebSocketBroadcaster;
import com.syncdoc.collaboration.presence.model.Presence;
import com.syncdoc.collaboration.presence.repository.PresenceRepository;
import com.syncdoc.collaboration.presence.service.PresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration-level tests for the presence status update flow.
 *
 * These tests verify the derivation logic end-to-end without requiring
 * a running database or Redis instance, keeping CI fast and hermetic.
 */
@ExtendWith(MockitoExtension.class)
public class PresenceStatusIntegrationTest {

    @Mock private PresenceRepository presenceRepository;
    @Mock private MessageService messageService;
    @Mock private RedisWebSocketBroadcaster broadcaster;

    private PresenceService presenceService;

    @BeforeEach
    void setUp() {
        presenceService = new PresenceService(presenceRepository, messageService, broadcaster);
    }

    // -----------------------------------------------------------------------
    // Manual status override
    // -----------------------------------------------------------------------

    @Test
    void manualOfflineStatusShouldOverrideRecentActivity() {
        Instant now = Instant.now();
        Instant lastSeen = now.minusSeconds(10); // user was active 10 s ago

        Presence.Status derived = PresenceService.deriveStatus(
            lastSeen, Presence.Status.OFFLINE, now, Duration.ofMinutes(5));

        assertThat(derived).isEqualTo(Presence.Status.OFFLINE);
    }

    @Test
    void manualAwayShouldSupersedeOnlineActivity() {
        Instant now = Instant.now();
        Instant lastSeen = now.minusSeconds(30);

        Presence.Status derived = PresenceService.deriveStatus(
            lastSeen, Presence.Status.AWAY, now, Duration.ofMinutes(5));

        assertThat(derived).isEqualTo(Presence.Status.AWAY);
    }

    // -----------------------------------------------------------------------
    // Automatic status derivation
    // -----------------------------------------------------------------------

    @Test
    void userActiveWithinThresholdShouldBeOnline() {
        Instant now = Instant.now();
        Instant lastSeen = now.minus(Duration.ofMinutes(2));

        Presence.Status derived = PresenceService.deriveStatus(
            lastSeen, null, now, Duration.ofMinutes(5));

        assertThat(derived).isEqualTo(Presence.Status.ONLINE);
    }

    @Test
    void userExceedingThresholdShouldBecomeAway() {
        Instant now = Instant.now();
        Instant lastSeen = now.minus(Duration.ofMinutes(10));

        Presence.Status derived = PresenceService.deriveStatus(
            lastSeen, null, now, Duration.ofMinutes(5));

        assertThat(derived).isEqualTo(Presence.Status.AWAY);
    }

    @Test
    void nullLastSeenShouldResultInOffline() {
        Presence.Status derived = PresenceService.deriveStatus(
            null, null, Instant.now(), Duration.ofMinutes(5));

        assertThat(derived).isEqualTo(Presence.Status.OFFLINE);
    }

    // -----------------------------------------------------------------------
    // Boundary: exactly at threshold
    // -----------------------------------------------------------------------

    @Test
    void userAtExactThresholdBoundaryShouldBeOnline() {
        Instant now = Instant.now();
        // Duration.between is strictly greater-than in deriveStatus, so exactly
        // at the threshold should still be ONLINE.
        Instant lastSeen = now.minus(Duration.ofMinutes(5));

        Presence.Status derived = PresenceService.deriveStatus(
            lastSeen, null, now, Duration.ofMinutes(5));

        assertThat(derived).isEqualTo(Presence.Status.ONLINE);
    }

    // -----------------------------------------------------------------------
    // Mock-based: persistence + broadcast
    // -----------------------------------------------------------------------

    @Test
    void shouldPersistManualStatusAndBroadcastOnStatusChange() {
        Presence existing = new Presence();
        existing.setWorkspaceId("ws-1");
        existing.setUserId("u-1");
        existing.setStatus(Presence.Status.ONLINE);
        existing.setLastSeenAt(Instant.now());

        when(presenceRepository.findByWorkspaceAndUser("ws-1", "u-1"))
            .thenReturn(Optional.of(existing));

        Presence updated = presenceService.upsertStatus("ws-1", "u-1", Presence.Status.OFFLINE);

        assertThat(updated.getManualStatus()).isEqualTo(Presence.Status.OFFLINE);
        assertThat(updated.getStatus()).isEqualTo(Presence.Status.OFFLINE);

        verify(presenceRepository, times(1)).upsert(any(Presence.class));
        ArgumentCaptor<Presence> captor = ArgumentCaptor.forClass(Presence.class);
        verify(broadcaster, times(1)).broadcast(
            eq("/topic/workspace/ws-1/presence"), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Presence.Status.OFFLINE);
    }

    @Test
    void shouldDebounceBroadcastOnConsecutiveHeartbeats() {
        Presence existing = new Presence();
        existing.setWorkspaceId("ws-1");
        existing.setUserId("u-1");
        existing.setStatus(Presence.Status.ONLINE);
        existing.setLastSeenAt(Instant.now().minusSeconds(5));

        when(presenceRepository.findByWorkspaceAndUser("ws-1", "u-1"))
            .thenReturn(Optional.of(existing));

        // First heartbeat broadcasts; second one within the debounce window is suppressed.
        presenceService.heartbeat("ws-1", "u-1");
        presenceService.heartbeat("ws-1", "u-1");

        verify(broadcaster, times(1)).broadcast(any(String.class), any(Presence.class));
    }
}
