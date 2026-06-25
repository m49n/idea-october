package dev.idea.october.navigation;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.PhpLanguage;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;

public final class OctoberPhpSectionAnnotator implements Annotator {
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element.getFirstChild() != null) {
            return;
        }

        PsiFile file = element.getContainingFile();
        VirtualFile virtualFile = findVirtualFile(file);
        if (file == null || virtualFile == null || !OctoberThemeTemplatePath.isThemeTemplate(Path.of(virtualFile.getPath()))) {
            return;
        }

        Optional<TextRange> phpSection = OctoberTemplateSectionParser.findPhpSection(file.getText());
        if (phpSection.isEmpty()) {
            return;
        }

        TextRange elementRange = element.getTextRange();
        TextRange phpRange = phpSection.get();
        if (elementRange == null || !elementRange.intersects(phpRange)) {
            return;
        }

        highlightElementRange(file, virtualFile, elementRange.intersection(phpRange), phpRange, holder);
    }

    private static void highlightElementRange(
        PsiFile file,
        VirtualFile virtualFile,
        TextRange targetRange,
        TextRange phpRange,
        AnnotationHolder holder
    ) {
        if (targetRange == null || targetRange.isEmpty()) {
            return;
        }

        Project project = file.getProject();
        SyntaxHighlighter highlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(
            PhpLanguage.INJECTABLE_INSTANCE,
            project,
            virtualFile
        );
        Lexer lexer = highlighter.getHighlightingLexer();
        lexer.start(phpRange.substring(file.getText()));

        int phpStartOffset = phpRange.getStartOffset();
        while (lexer.getTokenType() != null) {
            TextRange tokenRange = TextRange.create(
                phpStartOffset + lexer.getTokenStart(),
                phpStartOffset + lexer.getTokenEnd()
            );
            TextRange intersection = tokenRange.intersection(targetRange);
            if (intersection != null && !intersection.isEmpty()) {
                applyTokenHighlights(holder, highlighter, lexer, intersection);
            }
            lexer.advance();
        }
    }

    private static void applyTokenHighlights(
        AnnotationHolder holder,
        SyntaxHighlighter highlighter,
        Lexer lexer,
        TextRange range
    ) {
        for (TextAttributesKey key : highlighter.getTokenHighlights(lexer.getTokenType())) {
            holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
                .range(range)
                .textAttributes(key)
                .create();
        }
    }

    private static VirtualFile findVirtualFile(PsiFile file) {
        if (file == null) {
            return null;
        }

        VirtualFile virtualFile = file.getVirtualFile();
        return virtualFile == null ? file.getViewProvider().getVirtualFile() : virtualFile;
    }
}
