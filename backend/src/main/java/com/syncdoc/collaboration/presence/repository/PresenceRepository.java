package com.syncdoc.collaboration.presence.repository;

import com.syncdoc.collaboration.presence.model.Presence;
import com.syncdoc.collaboration.presence.model.Presence.Status;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PresenceRepository {

    private static final String PRESENCE_KEY_PREFIX = "presence:workspace:";

    private final HashOperations<String, String, String> hashOps;

    public PresenceRepository(RedisTemplate<String, String> redisTemplate) {
        this.hashOps = redisTemplate.opsForHash();
    }

    public void upsert(Presence presence) {
        String key = workspaceKey(presence.getWorkspaceId());
        hashOps.put(key, presence.getUserId() + ":status", presence.getStatus().name());
        hashOps.put(key, presence.getUserId() + ":lastSeen", String.valueOf(presence.getLastSeenAt().toEpochMilli()));
        if (presence.getManualStatus() != null) {
            hashOps.put(key, presence.getUserId() + ":manual", presence.getManualStatus().name());
        } else {
            hashOps.delete(key, presence.getUserId() + ":manual");
        }
    }

    public Optional<Presence> findByWorkspaceAndUser(String workspaceId, String userId) {
        String key = workspaceKey(workspaceId);
        String status = hashOps.get(key, userId + ":status");
        String lastSeen = hashOps.get(key, userId + ":lastSeen");
        String manual = hashOps.get(key, userId + ":manual");

        if (status == null || lastSeen == null) {
            return Optional.empty();
        }

        Presence presence = new Presence();
        presence.setWorkspaceId(workspaceId);
        presence.setUserId(userId);
        presence.setStatus(Status.valueOf(status));
        presence.setLastSeenAt(Instant.ofEpochMilli(Long.parseLong(lastSeen)));
        if (manual != null) {
            presence.setManualStatus(Status.valueOf(manual));
        }
        return Optional.of(presence);
    }

    public List<Presence> findByWorkspace(String workspaceId) {
        String key = workspaceKey(workspaceId);
        Map<String, String> all = hashOps.entries(key);
        List<Presence> result = new ArrayList<>();

        all.keySet().stream()
            .filter(k -> k.endsWith(":status"))
            .map(k -> k.substring(0, k.length() - ":status".length()))
            .distinct()
            .forEach(userId -> findByWorkspaceAndUser(workspaceId, userId).ifPresent(result::add));

        return result;
    }

    private String workspaceKey(String workspaceId) {
        return PRESENCE_KEY_PREFIX + workspaceId;
    }
}
