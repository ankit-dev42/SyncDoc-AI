package com.syncdoc.collaboration.subscription.integration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class UserSubscriptionsMigrationIntegrationTest {

    @Test
    void migrationShouldDefineExpectedUserSubscriptionsSchemaAndIndexes() throws IOException {
        Path migrationFile = Path.of("src", "main", "resources", "db", "migration", "V3__business_validation_entities.sql");
        String migration = Files.readString(migrationFile);

        assertThat(migration)
            .contains("CREATE TABLE IF NOT EXISTS user_subscriptions")
            .contains("user_id             VARCHAR(100) NOT NULL")
            .contains("subscription_tier   VARCHAR(20)  NOT NULL")
            .contains("stripe_customer_id  VARCHAR(100) NOT NULL")
            .contains("status              VARCHAR(20)  NOT NULL");

        assertThat(migration)
            .contains("CREATE UNIQUE INDEX IF NOT EXISTS idx_user_subscriptions_user_id ON user_subscriptions(user_id)")
            .contains("CREATE INDEX IF NOT EXISTS idx_user_subscriptions_status ON user_subscriptions(status)");
    }
}