package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OctoberTemplateSectionParser {
    private static final Pattern SECTION_SEPARATOR = Pattern.compile("(?m)^\\s*==\\s*$");

    private OctoberTemplateSectionParser() {
    }

    public static @NotNull Optional<TextRange> findPhpSection(@NotNull String fileText) {
        Matcher matcher = SECTION_SEPARATOR.matcher(fileText);
        if (!matcher.find()) {
            return Optional.empty();
        }

        int firstSeparatorEnd = matcher.end();
        if (!matcher.find()) {
            return Optional.empty();
        }

        int start = skipLineBreakAfter(fileText, firstSeparatorEnd);
        int end = trimLineBreakBefore(fileText, matcher.start());
        if (start >= end) {
            return Optional.empty();
        }

        return Optional.of(TextRange.create(start, end));
    }

    private static int skipLineBreakAfter(String text, int offset) {
        if (offset < text.length() && text.charAt(offset) == '\r') {
            offset++;
            if (offset < text.length() && text.charAt(offset) == '\n') {
                offset++;
            }
            return offset;
        }

        if (offset < text.length() && text.charAt(offset) == '\n') {
            return offset + 1;
        }

        return offset;
    }

    private static int trimLineBreakBefore(String text, int offset) {
        if (offset > 0 && text.charAt(offset - 1) == '\n') {
            offset--;
            if (offset > 0 && text.charAt(offset - 1) == '\r') {
                offset--;
            }
            return offset;
        }

        if (offset > 0 && text.charAt(offset - 1) == '\r') {
            return offset - 1;
        }

        return offset;
    }
}
