package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OctoberLayoutTarget {
    private static final Pattern SECTION_SEPARATOR = Pattern.compile("(?m)^\\s*==\\s*$");
    private static final Pattern LAYOUT_SETTING = Pattern.compile("(?m)^\\s*layout\\s*=\\s*(['\"])([^'\"]+)\\1");

    private OctoberLayoutTarget() {
    }

    public static @NotNull Optional<Match> find(@NotNull String fileText, int caretOffset) {
        if (caretOffset < 0 || caretOffset > fileText.length()) {
            return Optional.empty();
        }

        int configurationEndOffset = findConfigurationEndOffset(fileText);
        if (caretOffset > configurationEndOffset) {
            return Optional.empty();
        }

        Matcher matcher = LAYOUT_SETTING.matcher(fileText.substring(0, configurationEndOffset));
        while (matcher.find()) {
            int start = matcher.start(2);
            int end = matcher.end(2);
            if (caretOffset >= start && caretOffset <= end) {
                return Optional.of(new Match(matcher.group(2), TextRange.create(start, end)));
            }
        }

        return Optional.empty();
    }

    private static int findConfigurationEndOffset(@NotNull String fileText) {
        Matcher matcher = SECTION_SEPARATOR.matcher(fileText);
        return matcher.find() ? matcher.start() : fileText.length();
    }

    public record Match(@NotNull String layoutName, @NotNull TextRange rangeInFile) {
    }
}
