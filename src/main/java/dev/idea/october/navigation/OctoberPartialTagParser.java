package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OctoberPartialTagParser {
    private static final Pattern INLINE_PARTIAL_TAG = Pattern.compile("\\{%\\s*(?:partial|ajaxPartial)\\s+(['\"])([^'\"]+)\\1");
    private static final Pattern INLINE_PARTIAL_FUNCTION =
        Pattern.compile("\\b(?:partial|ajaxPartial)\\s*\\(\\s*(['\"])([^'\"]+)\\1");
    private static final Pattern PARTIAL_FUNCTION_PREFIX =
        Pattern.compile("(^|[^A-Za-z0-9_])(?:partial|ajaxPartial)\\s*\\(\\s*$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s");

    private OctoberPartialTagParser() {
    }

    public static @NotNull List<Match> findMatches(
        @NotNull String elementText,
        @NotNull String fileText,
        int startOffset,
        int endOffset,
        boolean leafElement
    ) {
        List<Match> matches = new ArrayList<>();

        if (leafElement) {
            extractQuotedStringReference(elementText, fileText, startOffset, endOffset)
                .forEach(matches::add);
        }

        Matcher matcher = INLINE_PARTIAL_TAG.matcher(elementText);
        while (matcher.find()) {
            String partialName = matcher.group(2);
            if (isPlausiblePartialName(partialName)) {
                matches.add(new Match(partialName, TextRange.create(matcher.start(2), matcher.end(2))));
            }
        }

        matcher = INLINE_PARTIAL_FUNCTION.matcher(elementText);
        while (matcher.find()) {
            String partialName = matcher.group(2);
            if (isPlausiblePartialName(partialName)) {
                matches.add(new Match(partialName, TextRange.create(matcher.start(2), matcher.end(2))));
            }
        }

        return matches;
    }

    private static @NotNull List<Match> extractQuotedStringReference(
        @NotNull String elementText,
        @NotNull String fileText,
        int startOffset,
        int endOffset
    ) {
        List<Match> matches = new ArrayList<>(1);

        if (hasMatchingQuotes(elementText)) {
            String partialName = elementText.substring(1, elementText.length() - 1);
            if (
                isPlausiblePartialName(partialName)
                    && isFirstStringArgumentOfPartialReference(fileText, startOffset)
            ) {
                matches.add(new Match(partialName, TextRange.create(1, elementText.length() - 1)));
            }
            return matches;
        }

        if (startOffset <= 0 || endOffset >= fileText.length()) {
            return matches;
        }

        char before = fileText.charAt(startOffset - 1);
        char after = fileText.charAt(endOffset);
        if (
            isQuote(before)
                && before == after
                && isPlausiblePartialName(elementText)
                && isFirstStringArgumentOfPartialReference(fileText, startOffset - 1)
        ) {
            matches.add(new Match(elementText, TextRange.create(0, elementText.length())));
        }

        return matches;
    }

    private static boolean isFirstStringArgumentOfPartialReference(@NotNull String fileText, int quoteStartOffset) {
        return isFirstStringArgumentOfPartialTag(fileText, quoteStartOffset)
            || isFirstStringArgumentOfPartialFunction(fileText, quoteStartOffset);
    }

    private static boolean isFirstStringArgumentOfPartialTag(@NotNull String fileText, int quoteStartOffset) {
        int tagStart = fileText.lastIndexOf("{%", quoteStartOffset);
        if (tagStart < 0) {
            return false;
        }

        int previousTagEnd = fileText.lastIndexOf("%}", quoteStartOffset);
        if (previousTagEnd > tagStart) {
            return false;
        }

        String prefix = fileText.substring(tagStart + 2, quoteStartOffset).trim();
        return "partial".equals(prefix) || "ajaxPartial".equals(prefix);
    }

    private static boolean isFirstStringArgumentOfPartialFunction(@NotNull String fileText, int quoteStartOffset) {
        String prefix = fileText.substring(Math.max(0, quoteStartOffset - 96), quoteStartOffset);
        return PARTIAL_FUNCTION_PREFIX.matcher(prefix).find();
    }

    private static boolean hasMatchingQuotes(@NotNull String value) {
        return value.length() >= 2
            && isQuote(value.charAt(0))
            && value.charAt(0) == value.charAt(value.length() - 1);
    }

    private static boolean isQuote(char value) {
        return value == '"' || value == '\'';
    }

    private static boolean isPlausiblePartialName(@NotNull String value) {
        return !value.isBlank() && !WHITESPACE.matcher(value).find();
    }

    public record Match(@NotNull String partialName, @NotNull TextRange rangeInElement) {
    }
}
