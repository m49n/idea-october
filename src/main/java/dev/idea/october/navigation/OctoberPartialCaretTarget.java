package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OctoberPartialCaretTarget {
    private static final Pattern PARTIAL_TAG = Pattern.compile("\\{%\\s*(?:partial|ajaxPartial)\\s+(['\"])([^'\"]+)\\1");
    private static final Pattern PARTIAL_FUNCTION = Pattern.compile("\\b(?:partial|ajaxPartial)\\s*\\(\\s*(['\"])([^'\"]+)\\1");

    private OctoberPartialCaretTarget() {
    }

    public static @NotNull Optional<Match> find(@NotNull String fileText, int caretOffset) {
        if (caretOffset < 0 || caretOffset > fileText.length()) {
            return Optional.empty();
        }

        Optional<Match> tagMatch = findMatch(PARTIAL_TAG, fileText, caretOffset);
        if (tagMatch.isPresent()) {
            return tagMatch;
        }

        return findMatch(PARTIAL_FUNCTION, fileText, caretOffset);
    }

    private static @NotNull Optional<Match> findMatch(
        @NotNull Pattern pattern,
        @NotNull String fileText,
        int caretOffset
    ) {
        Matcher matcher = pattern.matcher(fileText);
        while (matcher.find()) {
            int start = matcher.start(2);
            int end = matcher.end(2);
            if (caretOffset >= start && caretOffset <= end) {
                return Optional.of(new Match(matcher.group(2), TextRange.create(start, end)));
            }
        }

        return Optional.empty();
    }

    public record Match(@NotNull String partialName, @NotNull TextRange rangeInFile) {
    }
}
