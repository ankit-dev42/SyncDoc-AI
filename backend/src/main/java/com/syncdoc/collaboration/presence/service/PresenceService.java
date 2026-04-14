package com.syncdoc.collaboration.presence.service;

import com.syncdoc.collaboration.messaging.service.MessageService;
import com.syncdoc.collaboration.messaging.websocket.RedisWebSocketBroadcaster;
import com.syncdoc.collaboration.presence.model.Presence;
import com.syncdoc.collaboration.presence.repository.PresenceRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class PresenceService {

    private static final Duration DEFAULT_AWAY_THRESHOLD = Duration.ofMinutes(5);

    private final PresenceRepository presenceRepository;
    private final MessageService messageService;
    private final RedisWebSocketBroadcaster webSocketBroadcaster;

    public PresenceService(
        PresenceRepository presenceRepository,
        MessageService messageService,
        RedisWebSocketBroadcaster webSocketBroadcaster
    ) {
        this.presenceRepository = presenceRepository;
        this.messageService = messageService;
        this.webSocketBroadcaster = webSocketBroadcaster;
    }

    public Presence upsertStatus(String workspaceId, String userId, Presence.Status manualStatus) {
        Instant now = Instant.now();
        Presence presence = presenceRepository.findByWorkspaceAndUser(workspaceId, userId)
            .orElseGet(() -> {
                Presence p = new Presence();
                p.setWorkspaceId(workspaceId);
                p.setUserId(userId);
                return p;
            });

        presence.setLastSeenAt(now);
        presence.setManualStatus(manualStatus);
        presence.setStatus(deriveStatus(now, manualStatus, now, DEFAULT_AWAY_THRESHOLD));

        presenceRepository.upsert(presence);
        broadcast(workspaceId, presence);
        return presence;
    }

    public Presence heartbeat(String workspaceId, String userId) {
        Presence presence = upsertStatus(workspaceId, userId, null);
        presence.setStatus(Presence.Status.ONLINE);
        presenceRepository.upsert(presence);
        broadcast(workspaceId, presence);
        return presence;
    }

    public List<Presence> listWorkspacePresence(String workspaceId) {
        Instant now = Instant.now();
        List<Presence> all = presenceRepository.findByWorkspace(workspaceId);
        all.forEach(p -> {
            Presence.Status status = deriveStatus(p.getLastSeenAt(), p.getManualStatus(), now, DEFAULT_AWAY_THRESHOLD);
            p.setStatus(status);
        });
        return all;
    }

    public long unreadCount(String workspaceId, String channelId, long lastReadSequence) {
        return messageService.getUnreadCount(workspaceId, channelId, lastReadSequence);
    }

    public static Presence.Status deriveStatus(
        Instant lastSeenAt,
        Presence.Status manualStatus,
        Instant now,
        Duration awayThreshold
    ) {
        if (manualStatus != null) {
            return manualStatus;
        }
        if (lastSeenAt == null) {
            return Presence.Status.OFFLINE;
        }
        Duration idleTime = Duration.between(lastSeenAt, now);
        return idleTime.compareTo(awayThreshold) > 0 ? Presence.Status.AWAY : Presence.Status.ONLINE;
    }

    private void broadcast(String workspaceId, Presence presence) {
        webSocketBroadcaster.broadcast("/topic/workspace/" + workspaceId + "/presence", presence);
    }
}
