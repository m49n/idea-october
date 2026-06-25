package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class OctoberPhpSectionCompletionContext {
    private static final String DUMMY_IDENTIFIER = "IntellijIdeaRulezzz";

    private OctoberPhpSectionCompletionContext() {
    }

    public static @NotNull Optional<Context> find(@NotNull String documentText, int caretOffset) {
        Optional<TextRange> phpSection = OctoberTemplateSectionParser.findPhpSection(documentText);
        if (phpSection.isEmpty() || !containsCaret(phpSection.get(), caretOffset)) {
            return Optional.empty();
        }

        int prefixStart = caretOffset;
        while (prefixStart > phpSection.get().getStartOffset() && isPrefixCharacter(documentText.charAt(prefixStart - 1))) {
            prefixStart--;
        }

        String prefix = documentText.substring(prefixStart, caretOffset).replace(DUMMY_IDENTIFIER, "");
        return Optional.of(new Context(prefix, phpSection.get(), caretOffset));
    }

    private static boolean containsCaret(TextRange range, int caretOffset) {
        return caretOffset >= range.getStartOffset() && caretOffset <= range.getEndOffset();
    }

    private static boolean isPrefixCharacter(char character) {
        return Character.isLetterOrDigit(character)
            || character == '_'
            || character == '$'
            || character == '>'
            || character == '-'
            || character == '\\'
            || character == '\'';
    }

    public record Context(String prefix, TextRange phpSection, int caretOffset) {
    }
}
