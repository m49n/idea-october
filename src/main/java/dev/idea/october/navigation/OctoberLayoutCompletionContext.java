package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OctoberLayoutCompletionContext {
    private static final String INTELLIJ_DUMMY_IDENTIFIER = "IntellijIdeaRulezzz";
    private static final Pattern SECTION_SEPARATOR = Pattern.compile("(?m)^\\s*==\\s*$");
    private static final Pattern OPEN_LAYOUT_SETTING =
        Pattern.compile("(?m)^\\s*layout\\s*=\\s*(['\"])([^'\"]*)$");

    private OctoberLayoutCompletionContext() {
    }

    public static @NotNull Optional<Context> find(@NotNull String fileText, int caretOffset) {
        if (caretOffset < 0 || caretOffset > fileText.length()) {
            return Optional.empty();
        }

        int configurationEndOffset = findConfigurationEndOffset(fileText);
        if (caretOffset > configurationEndOffset) {
            return Optional.empty();
        }

        Matcher matcher = OPEN_LAYOUT_SETTING.matcher(fileText.substring(0, caretOffset));
        if (!matcher.find()) {
            return Optional.empty();
        }

        return Optional.of(new Context(cleanPrefix(matcher.group(2))));
    }

    private static int findConfigurationEndOffset(@NotNull String fileText) {
        Matcher matcher = SECTION_SEPARATOR.matcher(fileText);
        return matcher.find() ? matcher.start() : fileText.length();
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
