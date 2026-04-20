package com.syncdoc.collaboration.quality.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SecretExposureAuditTest {

    private static final Pattern QUOTED_LITERAL = Pattern.compile("[\"']([^\"']{6,})[\"']");
    private static final List<String> KEYWORDS = List.of("secret", "token", "password", "api_key", "apikey", "clientsecret", "webhook_secret");

    @Test
    void sourceTreesShouldNotContainHardcodedSecrets() throws IOException {
        List<String> findings = new ArrayList<>();

        scanTree(Path.of("src", "main"), findings);
        scanTree(Path.of("..", "frontend", "src"), findings);

        assertThat(findings)
            .as("Hardcoded secrets must not appear in tracked backend/frontend source files")
            .isEmpty();
    }

    private void scanTree(Path root, List<String> findings) throws IOException {
        if (!Files.exists(root)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(root)) {
            for (Path file : stream.filter(Files::isRegularFile).toList()) {
                if (!isScannable(file)) {
                    continue;
                }

                List<String> lines = Files.readAllLines(file);
                for (int index = 0; index < lines.size(); index++) {
                    String line = lines.get(index).trim();
                    if (isHardcodedSecret(line)) {
                        findings.add(file.normalize() + ":" + (index + 1) + " -> " + line);
                    }
                }
            }
        }
    }

    private boolean isScannable(Path file) {
        String name = file.toString();
        return name.endsWith(".java") || name.endsWith(".ts") || name.endsWith(".tsx")
            || name.endsWith(".js") || name.endsWith(".jsx") || name.endsWith(".yml")
            || name.endsWith(".yaml") || name.endsWith(".properties");
    }

    private boolean isHardcodedSecret(String line) {
        if (line.isBlank() || line.startsWith("//") || line.startsWith("/*") || line.startsWith("*")
                || line.startsWith("#") || line.startsWith("@")) {
            return false;
        }
        if (line.contains("${") || line.contains("Bearer ${") || line.contains("Authorization")) {
            return false;
        }

        String normalized = line.toLowerCase(Locale.ROOT).replace("-", "_");
        if (KEYWORDS.stream().noneMatch(normalized::contains)) {
            return false;
        }

        Matcher matcher = QUOTED_LITERAL.matcher(line);
        if (!matcher.find()) {
            return false;
        }

        String literal = matcher.group(1);
        return !literal.equalsIgnoreCase("sha256")
            && !literal.equalsIgnoreCase("hmacsha256")
            && !literal.startsWith("http")
            && !literal.contains("Content-Type")
            && !literal.contains("application/json")
            && !literal.contains(" ")                              // error messages have spaces; secrets don't
            && !literal.matches("[A-Z][A-Z_0-9]*")                // ALL_CAPS error codes (e.g. TOKEN_EXPIRED)
            && !literal.matches("[a-z]+[A-Z][a-zA-Z0-9]*");      // camelCase identifiers (e.g. refreshToken)
    }
}