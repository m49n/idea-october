package dev.idea.october.navigation;

import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public final class OctoberPartialReference extends PsiReferenceBase<PsiElement> {
    private final String partialName;

    public OctoberPartialReference(
        @NotNull PsiElement element,
        @NotNull TextRange rangeInElement,
        @NotNull String partialName
    ) {
        super(element, rangeInElement, true);
        this.partialName = partialName;
    }

    @Override
    public @Nullable PsiElement resolve() {
        PsiElement element = getElement();
        Project project = element.getProject();
        VirtualFile currentFile = findCurrentVirtualFile(element);
        if (currentFile == null) {
            return null;
        }

        return OctoberThemePartialResolver.findPartial(Path.of(currentFile.getPath()), partialName)
            .map(LocalFileSystem.getInstance()::findFileByNioFile)
            .map(virtualFile -> PsiManager.getInstance(project).findFile(virtualFile))
            .orElse(null);
    }

    @Override
    public @NotNull String getCanonicalText() {
        return partialName;
    }

    private static @Nullable VirtualFile findCurrentVirtualFile(@NotNull PsiElement element) {
        PsiFile topLevelFile = InjectedLanguageManager.getInstance(element.getProject()).getTopLevelFile(element);
        if (topLevelFile != null && topLevelFile.getVirtualFile() != null) {
            return topLevelFile.getVirtualFile();
        }

        PsiFile containingFile = element.getContainingFile();
        return containingFile == null ? null : containingFile.getVirtualFile();
    }
}
