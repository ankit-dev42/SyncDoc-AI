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

        scanTree(Path.of("src", "main"), findings, false);
        scanTree(Path.of("..", "frontend", "src"), findings, true);

        assertThat(findings)
            .as("Hardcoded secrets must not appear in tracked backend/frontend source files")
            .isEmpty();
    }

    private void scanTree(Path root, List<String> findings, boolean isFrontend) throws IOException {
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
                    if (isHardcodedSecret(line, isFrontend)) {
                        findings.add(file.normalize() + ":" + (index + 1) + " -> " + line);
                    }
                }
            }
        }
    }

    private boolean isScannable(Path file) {
        String name = file.toString();
        // Test/spec files intentionally contain synthetic fixture tokens — exclude from scan
        if (name.contains(".test.") || name.contains(".spec.")) {
            return false;
        }
        return name.endsWith(".java") || name.endsWith(".ts") || name.endsWith(".tsx")
            || name.endsWith(".js") || name.endsWith(".jsx") || name.endsWith(".yml")
            || name.endsWith(".yaml") || name.endsWith(".properties");
    }

    private boolean isHardcodedSecret(String line, boolean isFrontend) {
        if (line.isBlank() || line.startsWith("//") || line.startsWith("/*") || line.startsWith("*")
                || line.startsWith("#") || line.startsWith("@")) {
            return false;
        }
        if (line.contains("Bearer ${") || line.contains("Authorization")) {
            return false;
        }
        // Deny-list / forbidden-key declarations define key NAMES to block, not actual secret values
        if (line.contains("FORBIDDEN") || line.contains("BLOCKED") || line.contains("DENY_LIST")
                || line.contains("denyList") || line.contains("deniedKeys")) {
            return false;
        }
        if (isFrontend) {
            // HTML/JSX input attributes: type="password", type='text', etc. are not secret values
            if (line.matches(".*\\btype\\s*=\\s*[\"']\\w+[\"'].*")) {
                return false;
            }
            // Generic type annotations and interface fields: e.g. token: string, password: string
            if (line.matches(".*\\b(token|password|secret)\\s*[?:]\\s*string.*")) {
                return false;
            }
            // TypeScript generic type params: ApiResponse<TokenResponse>, Promise<TokenResponse>, etc.
            if (line.matches(".*<[A-Za-z]*(?i)(Token|Secret|Password)[A-Za-z]*>.*")) {
                return false;
            }
            // id/htmlFor attribute values are HTML field identifiers, not secret values
            if (line.matches(".*\\b(id|htmlFor)\\s*=\\s*[\"']\\w+[\"'].*")) {
                return false;
            }
            // autoComplete attribute values are HTML5 spec-defined strings (e.g. "current-password")
            if (line.matches(".*\\bautoComplete\\s*=.*")) {
                return false;
            }
            // React Hook Form register('fieldName', ...) — first arg is a schema key, not a secret
            if (line.matches(".*\\bregister\\(\\s*[\"']\\w+[\"'].*")) {
                return false;
            }
        }

        String normalized = line.toLowerCase(Locale.ROOT).replace("-", "_");
        if (KEYWORDS.stream().noneMatch(normalized::contains)) {
            return false;
        }

        Matcher matcher = QUOTED_LITERAL.matcher(line);
        while (matcher.find()) {
            String literal = matcher.group(1);

            // Skip env-var expression references like ${STRIPE_API_KEY} — not a hardcoded value
            int matchStart = matcher.start(1);
            int matchEnd   = matcher.end(1);
            boolean isEnvVarRef = matchStart >= 2
                    && line.charAt(matchStart - 2) == '$'
                    && line.charAt(matchStart - 1) == '{'
                    && matchEnd < line.length()
                    && line.charAt(matchEnd) == '}';
            if (isEnvVarRef) {
                continue;
            }

            // Skip URL/API path strings — not secrets
            if (literal.startsWith("/")) {
                continue;
            }

            if (!literal.equalsIgnoreCase("sha256")
                    && !literal.equalsIgnoreCase("hmacsha256")
                    && !literal.startsWith("http")
                    && !literal.contains("Content-Type")
                    && !literal.contains("application/json")
                    && !literal.contains(" ")                              // error messages have spaces; secrets don't
                    && !literal.matches("[A-Z][A-Z_0-9]*")                // ALL_CAPS error codes (e.g. TOKEN_EXPIRED)
                    && !literal.matches("[a-z]+[A-Z][a-zA-Z0-9]*")) {    // camelCase identifiers (e.g. refreshToken)
                return true;
            }
        }
        return false;
    }
}