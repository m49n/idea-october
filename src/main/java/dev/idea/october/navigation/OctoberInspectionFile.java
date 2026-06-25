package dev.idea.october.navigation;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

final class OctoberInspectionFile {
    private OctoberInspectionFile() {
    }

    static boolean shouldInspectThemeTemplate(@NotNull PsiFile file) {
        if (!isBasePsiFile(file)) {
            return false;
        }

        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
            virtualFile = file.getViewProvider().getVirtualFile();
        }

        return virtualFile != null && OctoberThemeTemplatePath.isThemeTemplate(Path.of(virtualFile.getPath()));
    }

    private static boolean isBasePsiFile(@NotNull PsiFile file) {
        PsiFile baseFile = file.getViewProvider().getPsi(file.getViewProvider().getBaseLanguage());
        return baseFile == null || baseFile == file;
    }
}
