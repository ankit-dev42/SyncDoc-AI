package com.syncdoc.collaboration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Abstract base class for all Testcontainers-based integration tests.
 *
 * <p>Starts a single shared PostgreSQL 16 container per JVM invocation — concrete
 * subclasses add {@code @SpringBootTest} and any other test-specific annotations.
 * The {@link DynamicPropertySource} override wires Spring's datasource to the
 * container's JDBC URL so Flyway and JPA pick it up automatically.
 */
// disabledWithoutDocker=true: IT tests skip gracefully when Docker socket is unavailable
// (e.g. Docker Desktop compatibility socket on macOS). Tests run normally in CI.
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideDataSourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Re-enable Flyway for IT tests (disabled in application-test.yml for unit tests)
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }
}
