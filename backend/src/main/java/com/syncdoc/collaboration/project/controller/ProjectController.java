package com.syncdoc.collaboration.project.controller;

import com.syncdoc.collaboration.common.dto.ApiResponse;
import com.syncdoc.collaboration.project.model.Project;
import com.syncdoc.collaboration.project.security.ProjectAccessService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@PreAuthorize("isAuthenticated()")
public class ProjectController {

    private final ProjectAccessService projectAccessService;

    public ProjectController(ProjectAccessService projectAccessService) {
        this.projectAccessService = projectAccessService;
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Project>> getProject(
        @PathVariable @NotBlank String projectId,
        Authentication authentication
    ) {
        String userId = String.valueOf(authentication.getPrincipal());
        Project project = projectAccessService.getOwnedProject(userId, projectId);
        return ResponseEntity.ok(ApiResponse.success("Project retrieved", project));
    }
}
