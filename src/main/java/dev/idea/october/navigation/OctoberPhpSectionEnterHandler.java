package dev.idea.october.navigation;

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public final class OctoberPhpSectionEnterHandler implements EnterHandlerDelegate {
    @Override
    public @NotNull Result preprocessEnter(
        @NotNull PsiFile file,
        @NotNull Editor editor,
        @NotNull Ref<Integer> caretOffsetRef,
        @NotNull Ref<Integer> caretAdvanceRef,
        @NotNull DataContext dataContext,
        EditorActionHandler originalHandler
    ) {
        if (
            OctoberPhpSectionCompletionContext
                .find(editor.getDocument().getText(), editor.getCaretModel().getOffset())
                .isEmpty()
        ) {
            return Result.Continue;
        }

        Lookup lookup = LookupManager.getActiveLookup(editor);
        if (!(lookup instanceof LookupImpl lookupImpl) || lookupImpl.getItems().isEmpty()) {
            return Result.Continue;
        }

        LookupElement item = lookup.getCurrentItem();
        if (item == null) {
            item = lookupImpl.getItems().getFirst();
        }
        LookupElement selectedItem = item;
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!lookupImpl.isLookupDisposed()) {
                lookupImpl.finishLookup(Lookup.NORMAL_SELECT_CHAR, selectedItem);
            }
        });
        return Result.Stop;
    }

    @Override
    public @NotNull Result postProcessEnter(@NotNull PsiFile file, @NotNull Editor editor, @NotNull DataContext dataContext) {
        int caretOffset = editor.getCaretModel().getOffset();
        if (!OctoberPhpSectionIndentation.adjustLineIndent(editor.getDocument(), caretOffset)) {
            return Result.Continue;
        }

        PsiDocumentManager.getInstance(file.getProject()).commitDocument(editor.getDocument());
        int lineNumber = editor.getDocument().getLineNumber(Math.min(caretOffset, editor.getDocument().getTextLength()));
        int lineStart = editor.getDocument().getLineStartOffset(lineNumber);
        int targetOffset = lineStart + OctoberPhpSectionIndentation.expectedIndent(editor.getDocument().getText(), lineStart);
        editor.getCaretModel().moveToOffset(Math.min(targetOffset, editor.getDocument().getTextLength()));
        return Result.Continue;
    }
}
