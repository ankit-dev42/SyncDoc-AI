package com.syncdoc.collaboration.project.security;

import com.syncdoc.collaboration.exception.BusinessValidationException;
import com.syncdoc.collaboration.project.model.Project;
import com.syncdoc.collaboration.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProjectAccessService {

    private final ProjectRepository projectRepository;

    public ProjectAccessService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Project getOwnedProject(String userId, String projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new BusinessValidationException(
                404,
                "PROJECT_NOT_FOUND",
                "Project not found: " + projectId
            ));

        if (!project.getOwnerId().equals(UUID.fromString(userId))) {
            throw new BusinessValidationException(
                403,
                "PROJECT_ACCESS_DENIED",
                "Access denied: you don't own this project."
            );
        }

        return project;
    }
}
