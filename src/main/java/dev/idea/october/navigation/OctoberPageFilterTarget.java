package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OctoberPageFilterTarget {
    private static final Pattern PAGE_FILTER =
        Pattern.compile("(['\"])([^'\"]+)\\1\\s*\\|\\s*page\\b");
    private static final Pattern PAGE_URL_FUNCTION =
        Pattern.compile("\\bpageUrl\\s*\\(\\s*(['\"])([^'\"]+)\\1");

    private OctoberPageFilterTarget() {
    }

    public static @NotNull Optional<Match> find(@NotNull String fileText, int caretOffset) {
        if (caretOffset < 0 || caretOffset > fileText.length()) {
            return Optional.empty();
        }

        Optional<Match> pageFilterMatch = findMatch(PAGE_FILTER, fileText, caretOffset);
        if (pageFilterMatch.isPresent()) {
            return pageFilterMatch;
        }

        return findMatch(PAGE_URL_FUNCTION, fileText, caretOffset);
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

    public record Match(@NotNull String pageName, @NotNull TextRange rangeInFile) {
    }
}
