package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OctoberComponentBlockParser {
    private static final String COMPONENT_IDENTIFIER = "[A-Za-z_][A-Za-z0-9_]*";
    private static final Pattern COMPONENT_BLOCK = Pattern.compile(
        "(?m)^[\\t ]*\\[(" + COMPONENT_IDENTIFIER + ")(?:[\\t ]+(" + COMPONENT_IDENTIFIER + "))?][\\t ]*$"
    );

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
            int componentStart = matcher.start(1);
            int componentEnd = matcher.end(1);
            int pageAliasStart = matcher.start(2);
            int pageAliasEnd = matcher.end(2);
            if (
                isInside(caretOffset, componentStart, componentEnd)
                    || (pageAliasStart >= 0 && isInside(caretOffset, pageAliasStart, pageAliasEnd))
            ) {
                return Optional.of(new Match(matcher.group(1), TextRange.create(componentStart, componentEnd)));
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

    private static boolean isInside(int offset, int start, int end) {
        return offset >= start && offset <= end;
    }

    public record Match(@NotNull String alias, @NotNull TextRange rangeInFile) {
    }
}
