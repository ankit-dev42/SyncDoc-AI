package com.syncdoc.collaboration.project.dto;

import com.syncdoc.collaboration.project.model.Project;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
    @NotBlank @Size(max = 255) String name,
    @NotNull Project.AccessControl accessControl
) {
}
