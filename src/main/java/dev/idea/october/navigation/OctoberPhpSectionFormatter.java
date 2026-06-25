package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;

public final class OctoberPhpSectionFormatter {
    private static final int INDENT_SIZE = 4;

    private OctoberPhpSectionFormatter() {
    }

    public static @NotNull String format(@NotNull String source) {
        String lineSeparator = source.contains("\r\n") ? "\r\n" : "\n";
        String normalized = source.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        StringBuilder formatted = new StringBuilder(source.length());
        int indentLevel = 0;

        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (i > 0) {
                formatted.append(lineSeparator);
            }

            if (trimmed.isEmpty()) {
                continue;
            }

            int leadingClosers = countLeadingClosingBraces(trimmed);
            int lineIndent = Math.max(0, indentLevel - leadingClosers);
            formatted.append(" ".repeat(lineIndent * INDENT_SIZE)).append(trimmed);

            indentLevel = Math.max(0, indentLevel + countBracesDelta(trimmed));
        }

        return formatted.toString();
    }

    private static int countLeadingClosingBraces(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == '}') {
            count++;
        }
        return count;
    }

    private static int countBracesDelta(String line) {
        int delta = 0;
        QuoteState quoteState = QuoteState.NONE;
        boolean escaped = false;

        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (quoteState == QuoteState.NONE && startsLineComment(line, i)) {
                break;
            }

            if (quoteState == QuoteState.NONE && (current == '\'' || current == '"')) {
                quoteState = current == '\'' ? QuoteState.SINGLE : QuoteState.DOUBLE;
                escaped = false;
                continue;
            }

            if (quoteState != QuoteState.NONE) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (
                    (quoteState == QuoteState.SINGLE && current == '\'')
                        || (quoteState == QuoteState.DOUBLE && current == '"')
                ) {
                    quoteState = QuoteState.NONE;
                }
                continue;
            }

            if (current == '{') {
                delta++;
            } else if (current == '}') {
                delta--;
            }
        }

        return delta;
    }

    private static boolean startsLineComment(String line, int offset) {
        return line.charAt(offset) == '#'
            || (line.charAt(offset) == '/' && offset + 1 < line.length() && line.charAt(offset + 1) == '/');
    }

    private enum QuoteState {
        NONE,
        SINGLE,
        DOUBLE
    }
}
