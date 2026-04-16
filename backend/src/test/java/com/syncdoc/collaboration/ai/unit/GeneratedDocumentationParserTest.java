package com.syncdoc.collaboration.ai.unit;

import com.syncdoc.collaboration.ai.parser.GeneratedDocumentationParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedDocumentationParserTest {

    private final GeneratedDocumentationParser parser = new GeneratedDocumentationParser();

    @Test
    void shouldExtractKeyChangesAndActionItemsFromMarkdownHeadings() {
        String markdown = """
            ## Summary
            Ignore this introduction.

            ## Key Changes
            - Added GitHub webhook verification
            - Enforced project ownership checks

            ## Action Items
            - Update the onboarding guide
            - Publish rollout notes
            """;

        GeneratedDocumentationParser.ParsedSections sections = parser.parse(markdown);

        assertThat(sections.keyChanges())
            .contains("Added GitHub webhook verification")
            .contains("Enforced project ownership checks");
        assertThat(sections.actionItems())
            .contains("Update the onboarding guide")
            .contains("Publish rollout notes");
    }

    @Test
    void shouldReturnPartialResultWithoutThrowingWhenMarkdownIsMalformedOrMissingASection() {
        String malformedMarkdown = """
            Key Changes
            Added a cache layer but forgot the markdown heading markers.

            ## Action Items
            - Add regression coverage for retry handling
            """;

        GeneratedDocumentationParser.ParsedSections sections = parser.parse(malformedMarkdown);

        assertThat(sections.keyChanges()).isBlank();
        assertThat(sections.actionItems())
            .contains("Add regression coverage for retry handling");
    }
}