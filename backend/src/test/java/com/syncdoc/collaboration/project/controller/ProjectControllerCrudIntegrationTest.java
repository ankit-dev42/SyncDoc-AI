package com.syncdoc.collaboration.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncdoc.collaboration.exception.BusinessValidationException;
import com.syncdoc.collaboration.exception.GlobalExceptionHandler;
import com.syncdoc.collaboration.project.dto.CreateProjectRequest;
import com.syncdoc.collaboration.project.dto.UpdateProjectRequest;
import com.syncdoc.collaboration.project.model.Project;
import com.syncdoc.collaboration.project.repository.ProjectRepository;
import com.syncdoc.collaboration.project.security.ProjectAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectController CRUD tests")
class ProjectControllerCrudIntegrationTest {

    @Mock private ProjectAccessService projectAccessService;
    @Mock private ProjectRepository projectRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String OWNER_ID = "00000000-0000-0000-0000-000000000001";
    private final UsernamePasswordAuthenticationToken ownerAuth =
        new UsernamePasswordAuthenticationToken(OWNER_ID, null, List.of());

    @BeforeEach
    void setUp() {
        ProjectController controller = new ProjectController(projectAccessService, projectRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("POST / creates project and returns 201")
    void createProject_returns201() throws Exception {
        Project saved = project("proj-1", UUID.randomUUID(), "My Project");
        when(projectRepository.save(any())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/projects")
                .principal(ownerAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateProjectRequest("My Project", Project.AccessControl.PRIVATE))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.name").value("My Project"));
    }

    @Test
    @DisplayName("GET / returns paginated project list")
    void listProjects_returnsPaginatedResults() throws Exception {
        Project p = project("proj-1", UUID.randomUUID(), "Alpha");
        when(projectRepository.findByOwnerIdOrderByCreatedAtDesc(any(UUID.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/projects").principal(ownerAuth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("PUT /{id} updates project and returns 200")
    void updateProject_returns200() throws Exception {
        Project existing = project("proj-1", UUID.randomUUID(), "Original");
        Project updated = project("proj-1", UUID.randomUUID(), "Updated");
        when(projectAccessService.getOwnedProject(OWNER_ID, "proj-1")).thenReturn(existing);
        when(projectRepository.save(any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/projects/proj-1")
                .principal(ownerAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProjectRequest("Updated", Project.AccessControl.PUBLIC))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Updated"));
    }

    @Test
    @DisplayName("DELETE /{id} returns 204")
    void deleteProject_returns204() throws Exception {
        Project existing = project("proj-1", UUID.randomUUID(), "ToDelete");
        when(projectAccessService.getOwnedProject(OWNER_ID, "proj-1")).thenReturn(existing);

        mockMvc.perform(delete("/api/v1/projects/proj-1").principal(ownerAuth))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /{id} returns 403 when not owner")
    void updateProject_notOwner_returns403() throws Exception {
        when(projectAccessService.getOwnedProject(OWNER_ID, "proj-1"))
            .thenThrow(new BusinessValidationException(403, "ACCESS_DENIED", "Not owner"));

        mockMvc.perform(put("/api/v1/projects/proj-1")
                .principal(ownerAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProjectRequest("Hack", Project.AccessControl.PUBLIC))))
            .andExpect(status().isForbidden());
    }

    private Project project(String id, UUID ownerId, String name) {
        Project p = new Project();
        p.setId(id);
        p.setOwnerId(ownerId);
        p.setName(name);
        p.setAccessControl(Project.AccessControl.PRIVATE);
        return p;
    }
}
