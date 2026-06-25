package dev.idea.october.navigation;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;

public final class OctoberPhpSectionDocumentFormatter {
    private OctoberPhpSectionDocumentFormatter() {
    }

    public static int format(@NotNull PsiFile file, @NotNull TextRange rangeToReformat) {
        VirtualFile virtualFile = findVirtualFile(file);
        if (virtualFile == null || !OctoberThemeTemplatePath.isThemeTemplate(Path.of(virtualFile.getPath()))) {
            return 0;
        }

        Optional<TextRange> phpSection = OctoberTemplateSectionParser.findPhpSection(file.getText());
        if (phpSection.isEmpty() || !rangeToReformat.intersects(phpSection.get())) {
            return 0;
        }

        TextRange phpRange = phpSection.get();
        String original = phpRange.substring(file.getText());
        String formatted = OctoberPhpSectionFormatter.format(original);
        if (original.equals(formatted)) {
            return 0;
        }

        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(file.getProject());
        Document document = documentManager.getDocument(file);
        if (document == null) {
            return 0;
        }

        document.replaceString(phpRange.getStartOffset(), phpRange.getEndOffset(), formatted);
        documentManager.commitDocument(document);

        return formatted.length() - original.length();
    }

    public static boolean canFormat(@NotNull PsiFile file) {
        VirtualFile virtualFile = findVirtualFile(file);
        return virtualFile != null
            && OctoberThemeTemplatePath.isThemeTemplate(Path.of(virtualFile.getPath()))
            && OctoberTemplateSectionParser.findPhpSection(file.getText()).isPresent();
    }

    private static VirtualFile findVirtualFile(PsiFile file) {
        VirtualFile virtualFile = file.getVirtualFile();
        return virtualFile == null ? file.getViewProvider().getVirtualFile() : virtualFile;
    }
}
