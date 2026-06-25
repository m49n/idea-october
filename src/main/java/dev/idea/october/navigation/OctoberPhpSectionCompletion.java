package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.util.TextRange;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OctoberPhpSectionCompletion {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$([A-Za-z_][A-Za-z0-9_]*)\\b");
    private static final Set<String> IGNORED_VARIABLES = Set.of(
        "this",
        "_GET",
        "_POST",
        "_REQUEST",
        "_SERVER",
        "_SESSION",
        "_COOKIE",
        "_FILES",
        "_ENV",
        "GLOBALS"
    );

    private static final List<Item> ITEMS = List.of(
        snippet("onInit", "()", "October lifecycle", 1),
        snippet("onStart", "()", "October lifecycle", 1),
        snippet("onEnd", "()", "October lifecycle", 1),
        snippet("onBeforePageStart", "()", "October lifecycle", 1),
        snippet("$this['']", "", "October page variable", -2),
        snippet("$this->param('')", "", "October route parameter", -2),
        snippet("$this->page['']", "", "October page variable", -2),
        snippet("$this->property('')", "", "October component property", -2),
        snippet("function", " ()\n{\n    \n}", "PHP", 3),
        snippet("if", " () {\n    \n}", "PHP", 2),
        snippet("foreach", " () {\n    \n}", "PHP", 2),
        keyword("use", "PHP"),
        keyword("return", "PHP"),
        keyword("true", "PHP"),
        keyword("false", "PHP"),
        keyword("null", "PHP")
    );

    private OctoberPhpSectionCompletion() {
    }

    public static @NotNull List<Item> items() {
        return ITEMS;
    }

    public static @NotNull List<String> localVariables(
        @NotNull String documentText,
        @NotNull TextRange phpSection,
        int caretOffset
    ) {
        int endOffset = Math.min(Math.min(caretOffset, phpSection.getEndOffset()), documentText.length());
        if (endOffset <= phpSection.getStartOffset()) {
            return List.of();
        }

        String textBeforeCaret = documentText.substring(phpSection.getStartOffset(), endOffset);
        Matcher matcher = VARIABLE_PATTERN.matcher(textBeforeCaret);
        LinkedHashSet<String> variables = new LinkedHashSet<>();
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!IGNORED_VARIABLES.contains(name)) {
                variables.add("$" + name);
            }
        }

        return new ArrayList<>(variables);
    }

    private static Item keyword(String lookupString, String typeText) {
        return new Item(lookupString, "", typeText, 0);
    }

    private static Item snippet(String lookupString, String tailText, String typeText, int caretShift) {
        return new Item(lookupString, tailText, typeText, caretShift);
    }

    public record Item(String lookupString, String tailText, String typeText, int caretShift) {
    }
}
