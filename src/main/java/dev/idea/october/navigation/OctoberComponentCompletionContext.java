package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OctoberComponentCompletionContext {
    private static final String INTELLIJ_DUMMY_IDENTIFIER = "IntellijIdeaRulezzz";
    private static final Pattern COMPONENT_BLOCK_PREFIX =
        Pattern.compile("(?m)^\\s*\\[([A-Za-z_][A-Za-z0-9_]*)?$");

    private OctoberComponentCompletionContext() {
    }

    public static @NotNull Optional<Context> find(@NotNull String fileText, int caretOffset) {
        if (caretOffset < 0 || caretOffset > fileText.length()) {
            return Optional.empty();
        }

        int configEnd = fileText.indexOf("==");
        if (configEnd < 0) {
            configEnd = fileText.length();
        }
        if (caretOffset > configEnd) {
            return Optional.empty();
        }

        Matcher matcher = COMPONENT_BLOCK_PREFIX.matcher(fileText.substring(0, caretOffset));
        if (!matcher.find()) {
            return Optional.empty();
        }

        String prefix = matcher.group(1) == null ? "" : matcher.group(1);
        return Optional.of(new Context(cleanPrefix(prefix)));
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
