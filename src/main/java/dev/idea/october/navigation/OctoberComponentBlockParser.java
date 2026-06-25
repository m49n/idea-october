package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OctoberComponentBlockParser {
    private static final Pattern COMPONENT_BLOCK = Pattern.compile("(?m)^\\s*\\[([A-Za-z_][A-Za-z0-9_]*)]\\s*$");

    private OctoberComponentBlockParser() {
    }

    public static @NotNull Optional<Match> find(@NotNull String fileText, int caretOffset) {
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

        Matcher matcher = COMPONENT_BLOCK.matcher(fileText.substring(0, configEnd));
        while (matcher.find()) {
            int start = matcher.start(1);
            int end = matcher.end(1);
            if (caretOffset >= start && caretOffset <= end) {
                return Optional.of(new Match(matcher.group(1), TextRange.create(start, end)));
            }
        }

        return Optional.empty();
    }

    public static @NotNull List<Match> scan(@NotNull String fileText) {
        int configEnd = fileText.indexOf("==");
        if (configEnd < 0) {
            configEnd = fileText.length();
        }

        List<Match> matches = new ArrayList<>();
        Matcher matcher = COMPONENT_BLOCK.matcher(fileText.substring(0, configEnd));
        while (matcher.find()) {
            matches.add(new Match(
                matcher.group(1),
                TextRange.create(matcher.start(1), matcher.end(1))
            ));
        }

        return matches;
    }

    public record Match(@NotNull String alias, @NotNull TextRange rangeInFile) {
    }
}
