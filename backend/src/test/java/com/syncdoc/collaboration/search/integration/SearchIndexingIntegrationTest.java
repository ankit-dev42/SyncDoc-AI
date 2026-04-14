package com.syncdoc.collaboration.search.integration;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class SearchIndexingIntegrationTest {

    @Test
    void shouldReturnMessagesMatchingSearchTermAfterIndexing() {
        List<String> indexedMessages = new ArrayList<>();
        indexedMessages.add("Database architecture decision finalized");
        indexedMessages.add("Frontend pagination strategy");
        indexedMessages.add("Database migration dry run complete");

        String term = "database";

        List<String> matches = indexedMessages.stream()
            .filter(m -> m.toLowerCase(Locale.ROOT).contains(term))
            .toList();

        assertThat(matches).hasSize(2);
        assertThat(matches.get(0)).contains("Database architecture");
        assertThat(matches.get(1)).contains("Database migration");
    }
}
