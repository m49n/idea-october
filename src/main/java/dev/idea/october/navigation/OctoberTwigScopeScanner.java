package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OctoberTwigScopeScanner {
    private static final String IDENTIFIER = "[A-Za-z_][A-Za-z0-9_]*";
    private static final Pattern TWIG_TAG = Pattern.compile("\\{%\\s*(.*?)\\s*%}", Pattern.DOTALL);
    private static final Pattern SET_TAG = Pattern.compile("^set\\s+(" + IDENTIFIER + ")\\s*=");
    private static final Pattern FOR_TAG = Pattern.compile(
        "^for\\s+(" + IDENTIFIER + ")(?:\\s*,\\s*(" + IDENTIFIER + "))?\\s+in\\b"
    );
    private static final Pattern END_FOR_TAG = Pattern.compile("^endfor\\b");

    private OctoberTwigScopeScanner() {
    }

    public static @NotNull Set<String> visibleVariables(@NotNull String fileText, int caretOffset) {
        if (caretOffset < 0 || caretOffset > fileText.length()) {
            return Set.of();
        }

        Set<String> declaredVariables = new LinkedHashSet<>();
        Deque<Set<String>> loopScopes = new ArrayDeque<>();
        Matcher tagMatcher = TWIG_TAG.matcher(fileText);
        tagMatcher.region(0, caretOffset);
        while (tagMatcher.find()) {
            String tagBody = tagMatcher.group(1).trim();

            Matcher setMatcher = SET_TAG.matcher(tagBody);
            if (setMatcher.find()) {
                declaredVariables.add(setMatcher.group(1));
                continue;
            }

            Matcher forMatcher = FOR_TAG.matcher(tagBody);
            if (forMatcher.find()) {
                Set<String> loopVariables = new LinkedHashSet<>();
                loopVariables.add(forMatcher.group(1));
                if (forMatcher.group(2) != null) {
                    loopVariables.add(forMatcher.group(2));
                }
                loopScopes.addLast(loopVariables);
                continue;
            }

            if (END_FOR_TAG.matcher(tagBody).find() && !loopScopes.isEmpty()) {
                loopScopes.removeLast();
            }
        }

        Set<String> result = new LinkedHashSet<>(declaredVariables);
        for (Set<String> loopScope : loopScopes) {
            result.addAll(loopScope);
        }
        return Collections.unmodifiableSet(result);
    }
}
