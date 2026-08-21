package dev.idea.october.navigation;

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupFocusDegree;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public final class OctoberPhpSectionTypedHandler extends TypedHandlerDelegate {
    @Override
    public @NotNull Result charTyped(char character, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        int caretOffset = editor.getCaretModel().getOffset();
        if (character == '}') {
            OctoberPhpSectionIndentation.adjustLineIndent(editor.getDocument(), caretOffset);
            PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
        }

        if (shouldAutoPopup(character, editor, caretOffset)) {
            scheduleCompletionPopup(project, editor);
        }

        return Result.CONTINUE;
    }

    private static void scheduleCompletionPopup(Project project, Editor editor) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed() || editor.isDisposed() || LookupManager.getActiveLookup(editor) != null) {
                return;
            }

            PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
            if (!hasAutoPopupCompletionContext(editor)) {
                return;
            }

            new CodeCompletionHandlerBase(CompletionType.BASIC, false, true, true)
                .invokeCompletion(project, editor);
            selectFirstLookupItem(editor);
        });
    }

    private static void selectFirstLookupItem(Editor editor) {
        Lookup lookup = LookupManager.getActiveLookup(editor);
        if (!(lookup instanceof LookupImpl lookupImpl) || lookupImpl.getItems().isEmpty()) {
            return;
        }

        lookupImpl.setCurrentItem(lookupImpl.getItems().getFirst());
        lookupImpl.setLookupFocusDegree(LookupFocusDegree.FOCUSED);
    }

    private static boolean shouldAutoPopup(char character, Editor editor, int caretOffset) {
        String documentText = editor.getDocument().getText();
        boolean existingContext = OctoberPhpSectionCompletionContext.find(documentText, caretOffset).isPresent()
            || OctoberComponentCompletionContext.find(documentText, caretOffset).isPresent()
            || OctoberComponentPropertyCompletionContext.find(documentText, caretOffset).isPresent();
        if (existingContext && isExistingCompletionTrigger(character)) {
            return true;
        }

        boolean twigContext = OctoberTwigTagCompletionContext.find(documentText, caretOffset).isPresent()
            || OctoberTwigExpressionCompletionContext.find(documentText, caretOffset).isPresent();
        return twigContext && isTwigCompletionTrigger(character);
    }

    private static boolean hasAutoPopupCompletionContext(Editor editor) {
        String documentText = editor.getDocument().getText();
        int caretOffset = editor.getCaretModel().getOffset();
        return OctoberPhpSectionCompletionContext.find(documentText, caretOffset).isPresent()
            || OctoberComponentCompletionContext.find(documentText, caretOffset).isPresent()
            || OctoberComponentPropertyCompletionContext.find(documentText, caretOffset).isPresent()
            || OctoberTwigTagCompletionContext.find(documentText, caretOffset).isPresent()
            || OctoberTwigExpressionCompletionContext.find(documentText, caretOffset).isPresent();
    }

    private static boolean isExistingCompletionTrigger(char character) {
        return Character.isLetterOrDigit(character)
            || character == '_'
            || character == '$'
            || character == '>'
            || character == '-'
            || character == '[';
    }

    private static boolean isTwigCompletionTrigger(char character) {
        return Character.isLetter(character)
            || character == '_'
            || character == '|'
            || character == '.';
    }
}
