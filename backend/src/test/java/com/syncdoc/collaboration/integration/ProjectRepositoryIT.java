package com.syncdoc.collaboration.integration;

import com.syncdoc.collaboration.AbstractIntegrationTest;
import com.syncdoc.collaboration.project.model.Project;
import com.syncdoc.collaboration.project.repository.ProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testcontainers integration test: verifies ProjectRepository operations against a real
 * PostgreSQL 16 container (T304).
 *
 * <p>Runs only during {@code mvn verify} (Failsafe — see T300 pom.xml configuration).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProjectRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    @DisplayName("saveAndFindAll: persisted projects appear in findAll()")
    void saveAndFindAll() {
        UUID ownerId = UUID.randomUUID();
        Project project = buildProject(ownerId, "My First Project");

        projectRepository.save(project);
        projectRepository.flush();

        List<Project> allProjects = projectRepository.findAll();
        assertThat(allProjects)
            .extracting(Project::getName)
            .contains("My First Project");
    }

    @Test
    @DisplayName("softDeletedProjectAbsentFromFindAll: @SQLRestriction filters deleted rows")
    void softDeletedProjectAbsentFromFindAll() {
        UUID ownerId = UUID.randomUUID();
        Project project = buildProject(ownerId, "To Be Soft-Deleted");
        projectRepository.save(project);
        projectRepository.flush();

        // soft-delete via repository delete (uses @SQLDelete UPDATE … SET deleted_at)
        projectRepository.delete(project);
        projectRepository.flush();

        List<Project> allVisible = projectRepository.findAll();
        assertThat(allVisible)
            .extracting(Project::getName)
            .doesNotContain("To Be Soft-Deleted");
    }

    @Test
    @DisplayName("findByOwnerId: returns only projects owned by the specified owner")
    void findByOwnerIdReturnsOnlyOwnedProjects() {
        UUID owner1 = UUID.randomUUID();
        UUID owner2 = UUID.randomUUID();

        projectRepository.save(buildProject(owner1, "Owner1 Project A"));
        projectRepository.save(buildProject(owner1, "Owner1 Project B"));
        projectRepository.save(buildProject(owner2, "Owner2 Project C"));
        projectRepository.flush();

        List<Project> owner1Projects = projectRepository.findByOwnerId(owner1);
        assertThat(owner1Projects)
            .hasSize(2)
            .extracting(Project::getOwnerId)
            .containsOnly(owner1);

        List<Project> owner2Projects = projectRepository.findByOwnerId(owner2);
        assertThat(owner2Projects)
            .hasSize(1)
            .extracting(Project::getName)
            .containsExactly("Owner2 Project C");
    }

    private Project buildProject(UUID ownerId, String name) {
        Project p = new Project();
        p.setOwnerId(ownerId);
        p.setName(name);
        p.setDescription("Integration test project");
        p.setAccessControl(Project.AccessControl.PRIVATE);
        return p;
    }
}
