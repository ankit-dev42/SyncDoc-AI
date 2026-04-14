package com.syncdoc.collaboration.tenancy.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class WorkspaceMembershipService {

    private static final String MEMBERSHIP_KEY_PREFIX = "workspace:members:";

    private final RedisTemplate<String, String> redisTemplate;
    private final boolean enforceRedisMembership;

    public WorkspaceMembershipService(
        RedisTemplate<String, String> redisTemplate,
        @Value("${security.workspace-membership.enforce-redis-membership:false}") boolean enforceRedisMembership
    ) {
        this.redisTemplate = redisTemplate;
        this.enforceRedisMembership = enforceRedisMembership;
    }

    public boolean isMember(String workspaceId, String userId) {
        if (workspaceId == null || workspaceId.isBlank() || userId == null || userId.isBlank()) {
            return false;
        }

        if (!enforceRedisMembership) {
            return true;
        }

        String key = MEMBERSHIP_KEY_PREFIX + workspaceId;
        Boolean isMember = redisTemplate.opsForSet().isMember(key, userId);
        return Boolean.TRUE.equals(isMember);
    }

    public void addMember(String workspaceId, String userId) {
        if (workspaceId == null || workspaceId.isBlank() || userId == null || userId.isBlank()) {
            return;
        }
        redisTemplate.opsForSet().add(MEMBERSHIP_KEY_PREFIX + workspaceId, userId);
    }

    public void removeMember(String workspaceId, String userId) {
        if (workspaceId == null || workspaceId.isBlank() || userId == null || userId.isBlank()) {
            return;
        }
        redisTemplate.opsForSet().remove(MEMBERSHIP_KEY_PREFIX + workspaceId, userId);
    }

    public List<String> listMembers(String workspaceId) {
        Set<String> members = redisTemplate.opsForSet().members(MEMBERSHIP_KEY_PREFIX + workspaceId);
        if (members == null) {
            return Collections.emptyList();
        }
        return members.stream().toList();
    }

    public List<String> listWorkspaceIdsForUser(String userId) {
        Set<String> keys = redisTemplate.keys(MEMBERSHIP_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        return keys.stream()
            .filter(k -> Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(k, userId)))
            .map(k -> k.substring(MEMBERSHIP_KEY_PREFIX.length()))
            .toList();
    }
}
