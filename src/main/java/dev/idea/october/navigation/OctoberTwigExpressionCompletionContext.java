package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OctoberTwigExpressionCompletionContext {
    private static final String INTELLIJ_DUMMY_IDENTIFIER = "IntellijIdeaRulezzz";
    private static final String IDENTIFIER = "[A-Za-z_][A-Za-z0-9_]*";
    private static final Pattern FILTER_PREFIX = Pattern.compile("\\|\\s*(" + IDENTIFIER + ")?$");
    private static final Pattern THIS_PROPERTY_PREFIX = Pattern.compile(
        "(?:^|[^A-Za-z0-9_])this\\.(" + IDENTIFIER + ")?$"
    );
    private static final Pattern VARIABLE_PREFIX = Pattern.compile("(" + IDENTIFIER + ")?$");
    private static final Pattern STATEMENT_NAME = Pattern.compile("^\\s*[A-Za-z_][A-Za-z0-9_]*\\s+");

    private OctoberTwigExpressionCompletionContext() {
    }

    public static @NotNull Optional<Context> find(@NotNull String fileText, int caretOffset) {
        if (caretOffset < 0 || caretOffset > fileText.length()) {
            return Optional.empty();
        }

        int outputStart = fileText.lastIndexOf("{{", Math.max(0, caretOffset - 1));
        int statementStart = fileText.lastIndexOf("{%", Math.max(0, caretOffset - 1));
        int expressionStart = Math.max(outputStart, statementStart);
        if (expressionStart < 0) {
            return Optional.empty();
        }

        boolean outputExpression = expressionStart == outputStart;
        String closingDelimiter = outputExpression ? "}}" : "%}";
        int previousExpressionEnd = fileText.lastIndexOf(closingDelimiter, Math.max(0, caretOffset - 1));
        if (previousExpressionEnd > expressionStart) {
            return Optional.empty();
        }

        String expression = cleanDummyIdentifier(fileText.substring(expressionStart + 2, caretOffset));
        if (!outputExpression && !STATEMENT_NAME.matcher(expression).find()) {
            return Optional.empty();
        }

        Matcher filterMatcher = FILTER_PREFIX.matcher(expression);
        if (filterMatcher.find()) {
            return Optional.of(new Context(Kind.FILTER, optionalGroup(filterMatcher, 1)));
        }

        Matcher propertyMatcher = THIS_PROPERTY_PREFIX.matcher(expression);
        if (propertyMatcher.find()) {
            return Optional.of(new Context(Kind.THIS_PROPERTY, optionalGroup(propertyMatcher, 1)));
        }

        Matcher variableMatcher = VARIABLE_PREFIX.matcher(expression);
        if (!variableMatcher.find()) {
            return Optional.empty();
        }

        String prefix = variableMatcher.group(1);
        if (prefix == null) {
            return isEmptyVariablePosition(expression)
                ? Optional.of(new Context(Kind.VARIABLE, ""))
                : Optional.empty();
        }

        int prefixStart = variableMatcher.start(1);
        if (prefixStart > 0) {
            char previousCharacter = expression.charAt(prefixStart - 1);
            if (previousCharacter == '.' || Character.isLetterOrDigit(previousCharacter) || previousCharacter == '_') {
                return Optional.empty();
            }
        }

        return Optional.of(new Context(Kind.VARIABLE, prefix));
    }

    private static boolean isEmptyVariablePosition(String expression) {
        String trimmed = expression.stripTrailing();
        if (trimmed.isBlank()) {
            return true;
        }

        char lastCharacter = trimmed.charAt(trimmed.length() - 1);
        return "=([{,:?+-*/~".indexOf(lastCharacter) >= 0;
    }

    private static String optionalGroup(Matcher matcher, int group) {
        String value = matcher.group(group);
        return value == null ? "" : value;
    }

    private static String cleanDummyIdentifier(String value) {
        return value.replace(INTELLIJ_DUMMY_IDENTIFIER, "");
    }

    public enum Kind {
        VARIABLE,
        FILTER,
        THIS_PROPERTY
    }

    public record Context(@NotNull Kind kind, @NotNull String prefix) {
    }
}
