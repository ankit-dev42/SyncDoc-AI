package com.syncdoc.collaboration.project.integration;

import com.syncdoc.collaboration.exception.BusinessValidationException;
import com.syncdoc.collaboration.exception.GlobalExceptionHandler;
import com.syncdoc.collaboration.exception.ResourceNotFoundException;
import com.syncdoc.collaboration.project.controller.ProjectController;
import com.syncdoc.collaboration.project.model.Project;
import com.syncdoc.collaboration.project.security.ProjectAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProjectControllerSecurityIntegrationTest {

    @Mock
    private ProjectAccessService projectAccessService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProjectController controller = new ProjectController(projectAccessService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void ownerShouldGet200() throws Exception {
        Project project = new Project();
        project.setId("project-1");
        project.setOwnerId("owner-1");
        project.setName("Owner Project");

        when(projectAccessService.getOwnedProject("owner-1", "project-1"))
            .thenReturn(project);

        mockMvc.perform(get("/api/v1/projects/project-1")
                .principal(new UsernamePasswordAuthenticationToken("owner-1", "N/A")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value("project-1"));
    }

    @Test
    void nonOwnerShouldGet403() throws Exception {
        when(projectAccessService.getOwnedProject("intruder", "project-1"))
            .thenThrow(new BusinessValidationException(
                403,
                "PROJECT_ACCESS_DENIED",
                "Access denied: you don't own this project."
            ));

        mockMvc.perform(get("/api/v1/projects/project-1")
                .principal(new UsernamePasswordAuthenticationToken("intruder", "N/A")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("PROJECT_ACCESS_DENIED"))
            .andExpect(jsonPath("$.message").value("Access denied: you don't own this project."));
    }

    @Test
    void missingProjectIdShouldGet404() throws Exception {
        when(projectAccessService.getOwnedProject(any(), eq("nonexistent-id")))
            .thenThrow(new ResourceNotFoundException("Project not found"));

        mockMvc.perform(get("/api/v1/projects/nonexistent-id")
                .principal(new UsernamePasswordAuthenticationToken("owner-1", "N/A")))
            .andExpect(status().isNotFound());
    }
}
