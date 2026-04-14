package com.syncdoc.collaboration.workspace.controller;

import com.syncdoc.collaboration.common.dto.ApiResponse;
import com.syncdoc.collaboration.tenancy.security.WorkspaceMembershipService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
@PreAuthorize("isAuthenticated()")
public class WorkspaceRestController {

    private final WorkspaceMembershipService workspaceMembershipService;

    public WorkspaceRestController(WorkspaceMembershipService workspaceMembershipService) {
        this.workspaceMembershipService = workspaceMembershipService;
    }

    @GetMapping
    public ApiResponse<List<WorkspaceDto>> listMyWorkspaces(Authentication authentication) {
        String userId = String.valueOf(authentication.getPrincipal());
        List<WorkspaceDto> workspaces = workspaceMembershipService.listWorkspaceIdsForUser(userId).stream()
            .map(id -> new WorkspaceDto(id, "Workspace " + id, workspaceMembershipService.listMembers(id).size()))
            .toList();

        return ApiResponse.success("Workspace list", workspaces);
    }

    @PostMapping("/{workspaceId}/members/{userId}")
    public ApiResponse<Void> addMember(
        @PathVariable @NotBlank String workspaceId,
        @PathVariable @NotBlank String userId
    ) {
        workspaceMembershipService.addMember(workspaceId, userId);
        return ApiResponse.success("Member added", null);
    }

    @DeleteMapping("/{workspaceId}/members/{userId}")
    public ApiResponse<Void> removeMember(
        @PathVariable @NotBlank String workspaceId,
        @PathVariable @NotBlank String userId
    ) {
        workspaceMembershipService.removeMember(workspaceId, userId);
        return ApiResponse.success("Member removed", null);
    }

    public record WorkspaceDto(String id, String name, int memberCount) {}
}
