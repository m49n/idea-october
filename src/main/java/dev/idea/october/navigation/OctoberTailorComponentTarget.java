package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OctoberTailorComponentTarget {
    private static final Set<String> COMPONENT_ALIASES = Set.of("collection", "section", "global");
    private static final String IDENTIFIER = "[A-Za-z_][A-Za-z0-9_]*";
    private static final Pattern TAILOR_COMPONENT = Pattern.compile(
        "(?im)^[\\t ]*\\[(collection|section|global)(?:[\\t ]+(" + IDENTIFIER + "))?][\\t ]*$"
    );
    private static final Pattern COMPONENT_BLOCK = Pattern.compile("(?m)^[\\t ]*\\[[^]\\r\\n]+][\\t ]*$");
    private static final Pattern HANDLE_PROPERTY = Pattern.compile(
        "(?im)^[\\t ]*handle[\\t ]*=[\\t ]*(?:(['\"])(?<quoted>[^'\"\\r\\n]+)\\1"
            + "|(?<unquoted>[^\\s'\"#;]+))[\\t ]*$"
    );

    private OctoberTailorComponentTarget() {
    }

    public static boolean isTailorComponentAlias(String alias) {
        return alias != null && COMPONENT_ALIASES.contains(alias.toLowerCase(java.util.Locale.ROOT));
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

        Matcher componentMatcher = TAILOR_COMPONENT.matcher(fileText);
        componentMatcher.region(0, configEnd);
        while (componentMatcher.find()) {
            int blockEnd = findBlockEnd(fileText, componentMatcher.end(), configEnd);
            Matcher handleMatcher = HANDLE_PROPERTY.matcher(fileText);
            handleMatcher.region(componentMatcher.end(), blockEnd);
            if (!handleMatcher.find()) {
                continue;
            }

            String handleGroup = handleMatcher.group("quoted") != null ? "quoted" : "unquoted";
            int handleStart = handleMatcher.start(handleGroup);
            int handleEnd = handleMatcher.end(handleGroup);

            if (
                isInside(caretOffset, componentMatcher.start(1), componentMatcher.end(1))
                    || isInsideOptionalGroup(caretOffset, componentMatcher, 2)
                    || isInside(caretOffset, handleStart, handleEnd)
            ) {
                return Optional.of(new Match(
                    handleMatcher.group(handleGroup),
                    TextRange.create(handleStart, handleEnd)
                ));
            }
        }

        return Optional.empty();
    }

    private static int findBlockEnd(String fileText, int blockStart, int configEnd) {
        Matcher nextComponent = COMPONENT_BLOCK.matcher(fileText);
        nextComponent.region(blockStart, configEnd);
        return nextComponent.find() ? nextComponent.start() : configEnd;
    }

    private static boolean isInsideOptionalGroup(int offset, Matcher matcher, int group) {
        int start = matcher.start(group);
        return start >= 0 && isInside(offset, start, matcher.end(group));
    }

    private static boolean isInside(int offset, int start, int end) {
        return offset >= start && offset <= end;
    }

    public record Match(@NotNull String handle, @NotNull TextRange rangeInFile) {
    }
}
