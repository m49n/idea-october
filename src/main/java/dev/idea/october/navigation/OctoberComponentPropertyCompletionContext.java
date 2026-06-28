package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OctoberComponentPropertyCompletionContext {
    private static final String INTELLIJ_DUMMY_IDENTIFIER = "IntellijIdeaRulezzz";
    private static final String COMPONENT_IDENTIFIER = "[A-Za-z_][A-Za-z0-9_]*";
    private static final String PROPERTY_IDENTIFIER = "[A-Za-z_][A-Za-z0-9_-]*";
    private static final Pattern COMPONENT_BLOCK = Pattern.compile(
        "(?m)^[\\t ]*\\[(" + COMPONENT_IDENTIFIER + ")(?:[\\t ]+(" + COMPONENT_IDENTIFIER + "))?][\\t ]*$"
    );
    private static final Pattern PROPERTY_KEY_PREFIX = Pattern.compile("^\\s*(" + PROPERTY_IDENTIFIER + ")?$");

    private OctoberComponentPropertyCompletionContext() {
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

        Optional<String> currentComponentAlias = findCurrentComponentAlias(fileText, caretOffset, configEnd);
        if (currentComponentAlias.isEmpty()) {
            return Optional.empty();
        }

        int lineStart = fileText.lastIndexOf('\n', Math.max(0, caretOffset - 1)) + 1;
        String lineBeforeCaret = fileText.substring(lineStart, caretOffset);
        Matcher matcher = PROPERTY_KEY_PREFIX.matcher(lineBeforeCaret);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String prefix = matcher.group(1) == null ? "" : matcher.group(1);
        return Optional.of(new Context(currentComponentAlias.get(), cleanPrefix(prefix)));
    }

    private static Optional<String> findCurrentComponentAlias(String fileText, int caretOffset, int configEnd) {
        Matcher matcher = COMPONENT_BLOCK.matcher(fileText.substring(0, configEnd));
        String currentAlias = null;
        int currentBlockEnd = -1;
        while (matcher.find() && matcher.end() <= caretOffset) {
            currentAlias = matcher.group(1);
            currentBlockEnd = matcher.end();
        }

        if (currentAlias == null || caretOffset <= currentBlockEnd) {
            return Optional.empty();
        }

        return Optional.of(currentAlias);
    }

    private static String cleanPrefix(String prefix) {
        if (prefix.endsWith(INTELLIJ_DUMMY_IDENTIFIER)) {
            return prefix.substring(0, prefix.length() - INTELLIJ_DUMMY_IDENTIFIER.length());
        }

        return prefix;
    }

    public record Context(@NotNull String componentAlias, @NotNull String prefix) {
    }
}
