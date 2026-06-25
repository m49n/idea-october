package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class OctoberPartialReferenceProvider extends PsiReferenceProvider {
    @Override
    public PsiReference @NotNull [] getReferencesByElement(
        @NotNull PsiElement element,
        @NotNull ProcessingContext context
    ) {
        String elementText = element.getText();
        if (elementText == null || elementText.isEmpty()) {
            return PsiReference.EMPTY_ARRAY;
        }

        TextRange elementRange = element.getTextRange();
        String fileText = element.getContainingFile().getText();
        boolean isLeafElement = element.getFirstChild() == null;

        if (!isLeafElement && elementText.length() > 512) {
            return PsiReference.EMPTY_ARRAY;
        }

        List<OctoberPartialTagParser.Match> matches = OctoberPartialTagParser.findMatches(
            elementText,
            fileText,
            elementRange.getStartOffset(),
            elementRange.getEndOffset(),
            isLeafElement
        );

        return matches.stream()
            .map(match -> new OctoberPartialReference(element, match.rangeInElement(), match.partialName()))
            .toArray(PsiReference[]::new);
    }
}
