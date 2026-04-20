package com.syncdoc.collaboration.project;

import com.syncdoc.collaboration.project.controller.ProjectController;
import com.syncdoc.collaboration.project.dto.ProjectDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.Entity;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that ProjectController returns only {@code ProjectDto} shape, and that
 * NO controller in the collaboration package exposes a JPA @Entity as a return type
 * (SC-P1-4).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectControllerDtoTest {

    @Autowired ApplicationContext applicationContext;
    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("ProjectController.getProject() return type is ProjectDto, not Project entity")
    void getProject_returnTypeIsProjectDto() throws Exception {
        Method getProject = ProjectController.class.getMethod("getProject", String.class,
            org.springframework.security.core.Authentication.class);
        // Walk ParameterizedType chain: ResponseEntity<ApiResponse<ProjectDto>>
        Type[] outerArgs = ((ParameterizedType) getProject.getGenericReturnType()).getActualTypeArguments();
        assertTrue(outerArgs.length > 0, "getProject must have a generic return type");
        // Inner type is ApiResponse<ProjectDto>
        Type innerType = outerArgs[0];
        if (innerType instanceof ParameterizedType pt) {
            Type[] innerArgs = pt.getActualTypeArguments();
            assertEquals(ProjectDto.class, innerArgs[0],
                "getProject must return ApiResponse<ProjectDto>, not a JPA entity");
        } else {
            fail("Expected generic ResponseEntity<ApiResponse<ProjectDto>>");
        }
    }

    @Test
    @DisplayName("No controller method in project/subscription packages returns a JPA @Entity class directly (SC-P1-4)")
    void noControllerReturnsJpaEntity() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        // Scope to Phase 1 security-sensitive packages only; other packages (e.g. messaging)
        // are governed by separate feature phases and have their own DTO migration plan.
        for (String pkg : List.of(
                "com.syncdoc.collaboration.project",
                "com.syncdoc.collaboration.subscription")) {
            for (var bd : scanner.findCandidateComponents(pkg)) {
                Class<?> controller = Class.forName(bd.getBeanClassName());
                for (Method method : controller.getDeclaredMethods()) {
                    assertNoEntityReturn(controller, method, method.getGenericReturnType());
                }
            }
        }
    }

    private void assertNoEntityReturn(Class<?> controller, Method method, Type type) {
        if (type instanceof Class<?> clazz) {
            if (clazz.isAnnotationPresent(Entity.class)) {
                fail(String.format("Controller %s.%s() returns JPA @Entity '%s' directly — use a DTO",
                    controller.getSimpleName(), method.getName(), clazz.getSimpleName()));
            }
        } else if (type instanceof ParameterizedType pt) {
            for (Type arg : pt.getActualTypeArguments()) {
                assertNoEntityReturn(controller, method, arg);
            }
        }
    }
}
