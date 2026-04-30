package com.syncdoc.collaboration.project.controller;

import com.syncdoc.collaboration.common.dto.ApiResponse;
import com.syncdoc.collaboration.project.dto.CreateProjectRequest;
import com.syncdoc.collaboration.project.dto.ProjectDto;
import com.syncdoc.collaboration.project.dto.UpdateProjectRequest;
import com.syncdoc.collaboration.project.model.Project;
import com.syncdoc.collaboration.project.repository.ProjectRepository;
import com.syncdoc.collaboration.project.security.ProjectAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@PreAuthorize("isAuthenticated()")
public class ProjectController {

    private final ProjectAccessService projectAccessService;
    private final ProjectRepository projectRepository;

    public ProjectController(ProjectAccessService projectAccessService, ProjectRepository projectRepository) {
        this.projectAccessService = projectAccessService;
        this.projectRepository = projectRepository;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectDto>> createProject(
        @Valid @RequestBody CreateProjectRequest request,
        Authentication authentication
    ) {
        String userId = authentication.getName();
        Project project = new Project();
        project.setOwnerId(UUID.fromString(userId));
        project.setName(request.name());
        project.setAccessControl(request.accessControl());
        Project saved = projectRepository.save(project);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Project created", ProjectDto.from(saved)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProjectDto>>> listProjects(
        Authentication authentication,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        String userId = authentication.getName();
        Pageable pageable = PageRequest.of(page, size);
        Page<ProjectDto> projects = projectRepository
            .findByOwnerIdOrderByCreatedAtDesc(UUID.fromString(userId), pageable)
            .map(ProjectDto::from);
        return ResponseEntity.ok(ApiResponse.success("Projects retrieved", projects));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectDto>> getProject(
        @PathVariable @NotBlank String projectId,
        Authentication authentication
    ) {
        String userId = authentication.getName();
        Project project = projectAccessService.getOwnedProject(userId, projectId);
        return ResponseEntity.ok(ApiResponse.success("Project retrieved", ProjectDto.from(project)));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectDto>> updateProject(
        @PathVariable @NotBlank String projectId,
        @Valid @RequestBody UpdateProjectRequest request,
        Authentication authentication
    ) {
        String userId = authentication.getName();
        Project project = projectAccessService.getOwnedProject(userId, projectId);
        project.setName(request.name());
        project.setAccessControl(request.accessControl());
        Project updated = projectRepository.save(project);
        return ResponseEntity.ok(ApiResponse.success("Project updated", ProjectDto.from(updated)));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
        @PathVariable @NotBlank String projectId,
        Authentication authentication
    ) {
        String userId = authentication.getName();
        Project project = projectAccessService.getOwnedProject(userId, projectId);
        projectRepository.delete(project);
        return ResponseEntity.noContent().build();
    }
}

