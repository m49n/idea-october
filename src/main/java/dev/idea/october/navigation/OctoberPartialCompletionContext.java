package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OctoberPartialCompletionContext {
    private static final String INTELLIJ_DUMMY_IDENTIFIER = "IntellijIdeaRulezzz";
    private static final Pattern OPEN_PARTIAL_ARGUMENT =
        Pattern.compile("\\{%\\s*(?:partial|ajaxPartial)\\s+(['\"])([^'\"]*)$");

    private OctoberPartialCompletionContext() {
    }

    public static @NotNull Optional<Context> find(@NotNull String fileText, int caretOffset) {
        if (caretOffset < 0 || caretOffset > fileText.length()) {
            return Optional.empty();
        }

        int tagStart = fileText.lastIndexOf("{%", caretOffset);
        if (tagStart < 0) {
            return Optional.empty();
        }

        int previousTagEnd = fileText.lastIndexOf("%}", caretOffset);
        if (previousTagEnd > tagStart) {
            return Optional.empty();
        }

        String beforeCaret = fileText.substring(tagStart, caretOffset);
        Matcher matcher = OPEN_PARTIAL_ARGUMENT.matcher(beforeCaret);
        if (!matcher.find()) {
            return Optional.empty();
        }

        return Optional.of(new Context(cleanPrefix(matcher.group(2))));
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
