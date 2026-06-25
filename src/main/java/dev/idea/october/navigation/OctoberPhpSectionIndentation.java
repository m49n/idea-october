package dev.idea.october.navigation;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class OctoberPhpSectionIndentation {
    private static final int INDENT_SIZE = 4;

    private OctoberPhpSectionIndentation() {
    }

    public static int expectedIndent(@NotNull String text, int offset) {
        Optional<TextRange> phpSection = OctoberTemplateSectionParser.findPhpSection(text);
        if (phpSection.isEmpty() || !containsOffset(phpSection.get(), offset)) {
            return 0;
        }

        int lineStart = lineStartOffset(text, offset);
        int level = 0;
        int scanOffset = phpSection.get().getStartOffset();
        while (scanOffset < lineStart) {
            int nextLineStart = nextLineStartOffset(text, scanOffset);
            String line = text.substring(scanOffset, Math.min(nextLineStart, lineStart));
            level = Math.max(0, level + countBracesDelta(line));
            scanOffset = nextLineStart;
        }

        String currentLine = currentLine(text, offset);
        if (currentLine.trim().startsWith("}")) {
            level = Math.max(0, level - 1);
        }

        return level * INDENT_SIZE;
    }

    public static boolean adjustLineIndent(@NotNull Document document, int offset) {
        String text = document.getText();
        Optional<TextRange> phpSection = OctoberTemplateSectionParser.findPhpSection(text);
        if (phpSection.isEmpty() || !containsOffset(phpSection.get(), offset)) {
            return false;
        }

        int lineNumber = document.getLineNumber(Math.min(offset, document.getTextLength()));
        int lineStart = document.getLineStartOffset(lineNumber);
        int lineEnd = document.getLineEndOffset(lineNumber);
        int contentStart = firstNonWhitespaceOffset(text, lineStart, lineEnd);
        String expectedIndent = " ".repeat(expectedIndent(text, lineStart));
        String currentIndent = text.substring(lineStart, contentStart);
        if (currentIndent.equals(expectedIndent)) {
            return false;
        }

        document.replaceString(lineStart, contentStart, expectedIndent);
        return true;
    }

    private static boolean containsOffset(TextRange range, int offset) {
        return offset >= range.getStartOffset() && offset <= range.getEndOffset();
    }

    private static int lineStartOffset(String text, int offset) {
        int boundedOffset = Math.min(offset, text.length());
        int previousLineBreak = text.lastIndexOf('\n', Math.max(0, boundedOffset - 1));
        return previousLineBreak < 0 ? 0 : previousLineBreak + 1;
    }

    private static int nextLineStartOffset(String text, int offset) {
        int lineBreak = text.indexOf('\n', offset);
        return lineBreak < 0 ? text.length() : lineBreak + 1;
    }

    private static String currentLine(String text, int offset) {
        int start = lineStartOffset(text, offset);
        int end = text.indexOf('\n', start);
        return text.substring(start, end < 0 ? text.length() : end);
    }

    private static int firstNonWhitespaceOffset(String text, int start, int end) {
        int offset = start;
        while (offset < end) {
            char character = text.charAt(offset);
            if (character != ' ' && character != '\t') {
                break;
            }
            offset++;
        }
        return offset;
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
