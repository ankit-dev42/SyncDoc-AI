package com.syncdoc.collaboration.ai.parser;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class GeneratedDocumentationParser {

    public ParsedSections parse(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return new ParsedSections("", "");
        }

        List<String> keyChangesLines = new ArrayList<>();
        List<String> actionItemsLines = new ArrayList<>();
        Section currentSection = Section.NONE;

        String[] lines = markdown.replace("\r\n", "\n").split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            Section headingSection = resolveHeading(trimmed);
            if (headingSection != Section.NONE) {
                currentSection = headingSection;
                continue;
            }

            if (isMarkdownHeading(trimmed)) {
                currentSection = Section.NONE;
                continue;
            }

            if (currentSection == Section.KEY_CHANGES) {
                appendLine(keyChangesLines, line);
            } else if (currentSection == Section.ACTION_ITEMS) {
                appendLine(actionItemsLines, line);
            }
        }

        return new ParsedSections(joinLines(keyChangesLines), joinLines(actionItemsLines));
    }

    private Section resolveHeading(String trimmedLine) {
        if (!trimmedLine.startsWith("#")) {
            return Section.NONE;
        }

        String headingText = trimmedLine.replaceFirst("^#{1,6}\\s*", "")
            .replace(":", "")
            .trim()
            .toLowerCase(Locale.ROOT);

        if (headingText.equals("key changes")) {
            return Section.KEY_CHANGES;
        }
        if (headingText.equals("action items")) {
            return Section.ACTION_ITEMS;
        }
        return Section.NONE;
    }

    private boolean isMarkdownHeading(String trimmedLine) {
        return trimmedLine.startsWith("#");
    }

    private void appendLine(List<String> lines, String line) {
        if (!line.trim().isBlank()) {
            lines.add(line.stripTrailing());
        }
    }

    private String joinLines(List<String> lines) {
        return String.join("\n", lines).trim();
    }

    public record ParsedSections(String keyChanges, String actionItems) {
    }

    private enum Section {
        NONE,
        KEY_CHANGES,
        ACTION_ITEMS
    }
}