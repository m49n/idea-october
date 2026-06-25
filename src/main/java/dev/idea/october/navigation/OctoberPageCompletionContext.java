package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OctoberPageCompletionContext {
    private static final String INTELLIJ_DUMMY_IDENTIFIER = "IntellijIdeaRulezzz";
    private static final Pattern PAGE_FILTER = Pattern.compile("(['\"])([^'\"]*)\\1\\s*\\|\\s*page\\b");
    private static final Pattern PAGE_URL_FUNCTION = Pattern.compile("\\bpageUrl\\s*\\(\\s*(['\"])([^'\"]*)\\1");

    private OctoberPageCompletionContext() {
    }

    public static @NotNull Optional<Context> find(@NotNull String fileText, int caretOffset) {
        if (caretOffset < 0 || caretOffset > fileText.length()) {
            return Optional.empty();
        }

        Optional<Context> pageFilterContext = findContext(PAGE_FILTER, fileText, caretOffset);
        if (pageFilterContext.isPresent()) {
            return pageFilterContext;
        }

        return findContext(PAGE_URL_FUNCTION, fileText, caretOffset);
    }

    private static @NotNull Optional<Context> findContext(
        @NotNull Pattern pattern,
        @NotNull String fileText,
        int caretOffset
    ) {
        Matcher matcher = pattern.matcher(fileText);
        while (matcher.find()) {
            int start = matcher.start(2);
            int end = matcher.end(2);
            if (caretOffset >= start && caretOffset <= end) {
                return Optional.of(new Context(cleanPrefix(fileText.substring(start, caretOffset))));
            }
        }

        return Optional.empty();
    }

    private static String cleanPrefix(String prefix) {
        if (prefix.endsWith(INTELLIJ_DUMMY_IDENTIFIER)) {
            return prefix.substring(0, prefix.length() - INTELLIJ_DUMMY_IDENTIFIER.length());
        }

        return prefix;
    }

    public record Context(@NotNull String prefix) {
    }
}
