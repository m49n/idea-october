package dev.idea.october.navigation;

import com.intellij.codeInsight.lookup.CharFilter;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class OctoberTwigFilterSpaceCharFilter extends CharFilter {
    @Override
    public @Nullable Result acceptChar(char character, int prefixLength, @NotNull Lookup lookup) {
        if (character != ' ') {
            return null;
        }

        Editor editor = lookup.getEditor();
        String documentText = editor.getDocument().getText();
        int caretOffset = editor.getCaretModel().getOffset();
        return OctoberTwigExpressionCompletionContext.find(documentText, caretOffset)
            .filter(context -> context.kind() == OctoberTwigExpressionCompletionContext.Kind.FILTER)
            .map(context -> Result.HIDE_LOOKUP)
            .orElse(null);
    }
}
