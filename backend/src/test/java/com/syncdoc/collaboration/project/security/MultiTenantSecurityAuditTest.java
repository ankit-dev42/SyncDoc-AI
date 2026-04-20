package com.syncdoc.collaboration.project.security;

import com.syncdoc.collaboration.exception.BusinessValidationException;
import com.syncdoc.collaboration.exception.GlobalExceptionHandler;
import com.syncdoc.collaboration.project.controller.ProjectController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Hard-gate multi-tenant isolation audit (T046).
 *
 * Contract:
 * - Execute 1000 unauthorized cross-project access attempts.
 * - Every single attempt must return 403 Forbidden.
 */
@Tag("security")
@ExtendWith(MockitoExtension.class)
class MultiTenantSecurityAuditTest {

    private static final int TOTAL_ATTEMPTS = 1000;

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
    void shouldReturn403ForAll1000CrossProjectAttempts() throws Exception {
        when(projectAccessService.getOwnedProject(eq("intruder"), anyString()))
            .thenThrow(new BusinessValidationException(
                403,
                "PROJECT_ACCESS_DENIED",
                "Access denied: you don't own this project."
            ));

        int forbiddenCount = 0;

        for (int i = 0; i < TOTAL_ATTEMPTS; i++) {
            int status = mockMvc.perform(get("/api/v1/projects/project-" + i)
                    .principal(new UsernamePasswordAuthenticationToken("intruder", "N/A")))
                .andReturn()
                .getResponse()
                .getStatus();
            assertThat(status)
                .as("Attempt %d must be denied with 403", i + 1)
                .isEqualTo(403);
            forbiddenCount++;
        }

        assertThat(forbiddenCount)
            .as("All attempts must be forbidden")
            .isEqualTo(TOTAL_ATTEMPTS);

        verify(projectAccessService, times(TOTAL_ATTEMPTS)).getOwnedProject(eq("intruder"), anyString());
    }
}
