package com.syncdoc.collaboration.presence.controller;

import com.syncdoc.collaboration.common.dto.ApiResponse;
import com.syncdoc.collaboration.presence.model.Presence;
import com.syncdoc.collaboration.presence.service.PresenceService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/presence")
@PreAuthorize("isAuthenticated()")
public class PresenceRestController {

    private final PresenceService presenceService;

    public PresenceRestController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @PostMapping("/status")
    public ApiResponse<Presence> setStatus(
        @PathVariable String workspaceId,
        @RequestBody SetPresenceRequest request
    ) {
        Presence presence = presenceService.upsertStatus(workspaceId, request.userId, request.status);
        return ApiResponse.success("Presence updated", presence);
    }

    @PostMapping("/heartbeat")
    public ApiResponse<Presence> heartbeat(
        @PathVariable String workspaceId,
        @RequestParam @NotBlank String userId
    ) {
        Presence presence = presenceService.heartbeat(workspaceId, userId);
        return ApiResponse.success("Heartbeat accepted", presence);
    }

    @GetMapping
    public ApiResponse<List<Presence>> list(@PathVariable String workspaceId) {
        return ApiResponse.success("Presence list", presenceService.listWorkspacePresence(workspaceId));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(
        @PathVariable String workspaceId,
        @RequestParam @NotBlank String channelId,
        @RequestParam long lastReadSequence
    ) {
        long unread = presenceService.unreadCount(workspaceId, channelId, lastReadSequence);
        return ApiResponse.success("Unread count", unread);
    }

    public static class SetPresenceRequest {
        public String userId;
        public Presence.Status status;
    }
}
