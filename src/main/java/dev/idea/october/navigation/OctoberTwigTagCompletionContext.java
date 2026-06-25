package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OctoberTwigTagCompletionContext {
    private static final String INTELLIJ_DUMMY_IDENTIFIER = "IntellijIdeaRulezzz";
    private static final Pattern OPEN_TAG_NAME =
        Pattern.compile("\\{%\\s*([A-Za-z][A-Za-z0-9_]*)?$");

    private OctoberTwigTagCompletionContext() {
    }

    public static @NotNull Optional<Context> find(@NotNull String fileText, int caretOffset) {
        if (caretOffset < 0 || caretOffset > fileText.length()) {
            return Optional.empty();
        }

        int tagStart = fileText.lastIndexOf("{%", caretOffset);
        if (tagStart < 0) {
            return Optional.empty();
        }

        int previousTagEnd = fileText.lastIndexOf("%}", Math.max(0, caretOffset - 1));
        if (previousTagEnd > tagStart) {
            return Optional.empty();
        }

        String beforeCaret = fileText.substring(tagStart, caretOffset);
        Matcher matcher = OPEN_TAG_NAME.matcher(beforeCaret);
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
