package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor;
import org.jetbrains.annotations.NotNull;

public final class OctoberPhpSectionPostFormatProcessor implements PostFormatProcessor {
    @Override
    public PsiElement processElement(@NotNull PsiElement source, @NotNull CodeStyleSettings settings) {
        PsiFile file = source.getContainingFile();
        if (file != null) {
            processText(file, source.getTextRange(), settings);
        }
        return source;
    }

    @Override
    public TextRange processText(@NotNull PsiFile file, @NotNull TextRange rangeToReformat, @NotNull CodeStyleSettings settings) {
        int delta = OctoberPhpSectionDocumentFormatter.format(file, rangeToReformat);
        return TextRange.create(
            rangeToReformat.getStartOffset(),
            Math.max(rangeToReformat.getStartOffset(), rangeToReformat.getEndOffset() + delta)
        );
    }
}
