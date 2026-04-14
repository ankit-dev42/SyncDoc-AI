package com.syncdoc.collaboration.ai.performance;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Accuracy hard-gate for SC-005 (T047/T048).
 *
 * Gate:
 * - Evaluate 20 fixture pairs (diff + expected summary).
 * - Average similarity score must be >= 0.95.
 */
@Tag("performance")
@Disabled("Scaffold only: wire to real AIProcessingService/OpenAI mock before enabling")
class AIExtractionAccuracyGateTest {

    private static final String FIXTURE_ROOT = "fixtures/ai-accuracy";
    private static final int REQUIRED_CASES = 20;
    private static final double MIN_AVERAGE_SCORE = 0.95;

    @Test
    void fixtureCorpusShouldContain20Cases() throws Exception {
        List<FixtureCase> cases = loadFixtureCases();
        assertThat(cases)
            .as("Fixture corpus should include exactly %d cases", REQUIRED_CASES)
            .hasSize(REQUIRED_CASES);
    }

    @Test
    void averageSimilarityShouldBeAtLeastPoint95() throws Exception {
        List<FixtureCase> cases = loadFixtureCases();

        double sum = 0.0;
        for (FixtureCase testCase : cases) {
            String actualSummary = generateSummaryForDiff(testCase.sampleDiff());
            double score = tokenJaccardSimilarity(actualSummary, testCase.expectedSummary());
            sum += score;
        }

        double averageScore = sum / cases.size();
        assertThat(averageScore)
            .as("Average similarity score across %d fixtures", cases.size())
            .isGreaterThanOrEqualTo(MIN_AVERAGE_SCORE);
    }

    private List<FixtureCase> loadFixtureCases() throws IOException, URISyntaxException {
        Path root = Paths.get(ClassLoader.getSystemResource(FIXTURE_ROOT).toURI());
        List<FixtureCase> cases = new ArrayList<>();

        try (var stream = Files.list(root)) {
            stream.filter(Files::isDirectory)
                .sorted()
                .forEach(caseDir -> {
                    try {
                        String diff = Files.readString(caseDir.resolve("sample_git_diff.txt"));
                        String expected = Files.readString(caseDir.resolve("expected_summary.md"));
                        cases.add(new FixtureCase(diff, expected));
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read fixture case: " + caseDir, e);
                    }
                });
        }

        return cases;
    }

    /**
     * Placeholder summary generator. Replace with real service invocation.
     */
    private String generateSummaryForDiff(String sampleDiff) {
        // TODO: Replace with AIProcessingService call (mocked OpenAI in test scope).
        return sampleDiff;
    }

    private double tokenJaccardSimilarity(String a, String b) {
        Set<String> left = tokenize(a);
        Set<String> right = tokenize(b);
        if (left.isEmpty() && right.isEmpty()) {
            return 1.0;
        }

        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);

        Set<String> union = new HashSet<>(left);
        union.addAll(right);

        return union.isEmpty() ? 1.0 : ((double) intersection.size() / union.size());
    }

    private Set<String> tokenize(String input) {
        String normalized = input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ");
        String[] parts = normalized.trim().split("\\s+");
        Set<String> tokens = new HashSet<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                tokens.add(part);
            }
        }
        return tokens;
    }

    private record FixtureCase(String sampleDiff, String expectedSummary) {}
}
