package com.syncdoc.collaboration.quality.runtime;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class Java17CompatibilityTest {

    private static final int JAVA_17_CLASS_FILE_VERSION = 61;
    private static final List<String> FORBIDDEN_JAVA_21_APIS = List.of(
        "SequencedCollection",
        "SequencedMap",
        "SequencedSet",
        "StringTemplate",
        "ScopedValue"
    );

    @Test
    void compiledBytecodeShouldTargetJava17() throws IOException {
        Path classesRoot = Path.of("target", "classes");
        assertThat(classesRoot).exists();

        try (Stream<Path> stream = Files.walk(classesRoot)) {
            List<Path> classFiles = stream
                .filter(path -> path.toString().endsWith(".class"))
                .toList();

            assertThat(classFiles).isNotEmpty();
            for (Path classFile : classFiles) {
                byte[] bytes = Files.readAllBytes(classFile);
                int majorVersion = ((bytes[6] & 0xFF) << 8) | (bytes[7] & 0xFF);
                assertThat(majorVersion)
                    .as("Class %s must compile to Java 17 bytecode", classesRoot.relativize(classFile))
                    .isEqualTo(JAVA_17_CLASS_FILE_VERSION);
            }
        }
    }

    @Test
    void sourceShouldNotReferenceKnownJava21OnlyApis() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java");

        try (Stream<Path> stream = Files.walk(sourceRoot)) {
            List<Path> javaFiles = stream
                .filter(path -> path.toString().endsWith(".java"))
                .toList();

            for (Path javaFile : javaFiles) {
                String content = Files.readString(javaFile);
                for (String forbiddenApi : FORBIDDEN_JAVA_21_APIS) {
                    assertThat(content)
                        .as("Source file %s must not reference Java 21 API %s", sourceRoot.relativize(javaFile), forbiddenApi)
                        .doesNotContain(forbiddenApi);
                }
            }
        }
    }
}