package com.syncdoc.collaboration.integration;

import com.syncdoc.collaboration.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testcontainers integration test: verifies all V1–V12 Flyway migrations apply cleanly
 * against a real PostgreSQL 16 container (T303).
 *
 * <p>Runs only during {@code mvn verify} (Failsafe plugin — see T300 pom.xml configuration).
 * {@code mvn test} does NOT start Docker.
 */
@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("All Flyway migrations apply without errors (no failed checksum rows)")
    void allMigrationsApplyCleanly() {
        Integer failedCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = false",
            Integer.class
        );
        assertThat(failedCount)
            .as("No Flyway migration should have failed")
            .isZero();
    }

    @Test
    @DisplayName("Required tables exist after all migrations")
    void requiredTablesExist() throws Exception {
        List<String> existingTables = new ArrayList<>();
        try (var conn = jdbcTemplate.getDataSource().getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            for (String table : List.of("users", "refresh_tokens", "projects", "user_subscriptions",
                "webhook_events", "sync_logs")) {
                try (var rs = meta.getTables(null, null, table, new String[]{"TABLE"})) {
                    if (rs.next()) {
                        existingTables.add(table);
                    }
                }
            }
        }
        assertThat(existingTables)
            .as("All required tables must exist after Flyway migrations")
            .containsExactlyInAnyOrder("users", "refresh_tokens", "projects", "user_subscriptions",
                "webhook_events", "sync_logs");
    }
}
