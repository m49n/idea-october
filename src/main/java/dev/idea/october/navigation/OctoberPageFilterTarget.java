package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OctoberPageFilterTarget {
    private static final Pattern PAGE_FILTER =
        Pattern.compile("(['\"])([^'\"]+)\\1\\s*\\|\\s*page\\b");

    private OctoberPageFilterTarget() {
    }

    public static @NotNull Optional<Match> find(@NotNull String fileText, int caretOffset) {
        if (caretOffset < 0 || caretOffset > fileText.length()) {
            return Optional.empty();
        }

        Matcher matcher = PAGE_FILTER.matcher(fileText);
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
