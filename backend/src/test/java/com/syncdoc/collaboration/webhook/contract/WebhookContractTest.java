package com.syncdoc.collaboration.webhook.contract;

import com.syncdoc.collaboration.webhook.dto.GithubEventPayload;
import com.syncdoc.collaboration.webhook.service.GithubEventSchemaValidator;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookContractTest {

    private static final String SIGNATURE_REGEX = "^sha256=[a-fA-F0-9]{64}$";

    private final GithubEventSchemaValidator validator = new GithubEventSchemaValidator();

    @Test
    void shouldValidateGithubSignatureHeaderFormatContract() {
        assertThat("sha256=9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08")
            .matches(SIGNATURE_REGEX);
        assertThat("sha1=invalid")
            .doesNotMatch(SIGNATURE_REGEX);
    }

    @Test
    void shouldValidateInternalEventSchemaContract() {
        GithubEventPayload valid = new GithubEventPayload(
            "evt-1",
            "push",
            "repo-1",
            "delivery-1",
            "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
            Instant.now(),
            "1.0"
        );

        GithubEventPayload invalid = new GithubEventPayload(
            "",
            "",
            "",
            "delivery-1",
            "invalid-hash",
            Instant.now(),
            ""
        );

        assertThat(validator.isValid(valid)).isTrue();
        assertThat(validator.isValid(invalid)).isFalse();
    }
}
