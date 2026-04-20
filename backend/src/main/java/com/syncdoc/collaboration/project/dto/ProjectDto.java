package com.syncdoc.collaboration.project.dto;

import com.syncdoc.collaboration.project.model.Project;

import java.time.Instant;

/**
 * Public-facing DTO for {@link Project}. Excludes internal fields such as
 * {@code ownerId} to prevent information leakage (SC-P1-4).
 *
 * @param id            project unique identifier
 * @param name          human-readable project name
 * @param accessControl access visibility setting (e.g. PRIVATE, PUBLIC)
 * @param createdAt     creation timestamp
 * @param updatedAt     last-modified timestamp
 */
public record ProjectDto(
    String id,
    String name,
    String accessControl,
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * Maps a {@link Project} entity to a {@link ProjectDto}.
     *
     * @param project the source entity
     * @return a DTO safe for API serialization
     */
    public static ProjectDto from(Project project) {
        return new ProjectDto(
            project.getId(),
            project.getName(),
            project.getAccessControl() != null ? project.getAccessControl().name() : null,
            project.getCreatedAt(),
            project.getUpdatedAt()
        );
    }
}
