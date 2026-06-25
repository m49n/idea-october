package dev.idea.october.navigation;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import java.nio.file.Path;

public final class OctoberPartialCompletionContributor extends CompletionContributor {
    @Override
    public void beforeCompletion(CompletionInitializationContext context) {
        String documentText = context.getEditor().getDocument().getText();
        if (OctoberPartialCompletionContext.find(documentText, context.getStartOffset()).isPresent()) {
            context.setDummyIdentifier("");
        }

        super.beforeCompletion(context);
    }

    @Override
    public void fillCompletionVariants(CompletionParameters parameters, CompletionResultSet result) {
        String documentText = parameters.getEditor().getDocument().getText();
        OctoberPartialCompletionContext.find(documentText, parameters.getOffset())
            .ifPresent(context -> addPartialCompletions(parameters, result, context.prefix()));

        super.fillCompletionVariants(parameters, result);
    }

    private static void addPartialCompletions(
        CompletionParameters parameters,
        CompletionResultSet result,
        String prefix
    ) {
        VirtualFile currentFile = findCurrentVirtualFile(parameters.getPosition());
        if (currentFile == null) {
            return;
        }

        CompletionResultSet prefixedResult = result.withPrefixMatcher(prefix).caseInsensitive();
        for (String partialName : OctoberThemePartialResolver.listPartialNames(Path.of(currentFile.getPath()))) {
            prefixedResult.addElement(
                LookupElementBuilder.create(partialName)
                    .withTypeText("October partial", true)
            );
        }
    }

    private static VirtualFile findCurrentVirtualFile(PsiElement position) {
        PsiFile topLevelFile = InjectedLanguageManager.getInstance(position.getProject()).getTopLevelFile(position);
        if (topLevelFile != null && topLevelFile.getVirtualFile() != null) {
            return topLevelFile.getVirtualFile();
        }

        PsiFile containingFile = position.getContainingFile();
        return containingFile == null ? null : containingFile.getVirtualFile();
    }
}
