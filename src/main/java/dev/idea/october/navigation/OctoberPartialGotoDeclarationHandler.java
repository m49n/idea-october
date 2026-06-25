package dev.idea.october.navigation;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public final class OctoberPartialGotoDeclarationHandler implements GotoDeclarationHandler {
    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(
        @Nullable PsiElement sourceElement,
        int offset,
        Editor editor
    ) {
        if (sourceElement == null || editor == null) {
            return null;
        }

        String documentText = editor.getDocument().getText();
        PsiElement[] partialTargets = OctoberPartialCaretTarget.find(documentText, offset)
            .flatMap(match -> resolvePartial(sourceElement, match.partialName()))
            .map(psiFile -> new PsiElement[]{psiFile})
            .orElse(null);
        if (partialTargets != null) {
            return partialTargets;
        }

        PsiElement[] layoutTargets = OctoberLayoutTarget.find(documentText, offset)
            .flatMap(match -> resolveLayout(sourceElement, match.layoutName()))
            .map(psiFile -> new PsiElement[]{psiFile})
            .orElse(null);
        if (layoutTargets != null) {
            return layoutTargets;
        }

        PsiElement[] contentTargets = OctoberContentTarget.find(documentText, offset)
            .flatMap(match -> resolveContent(sourceElement, match.contentName()))
            .map(psiFile -> new PsiElement[]{psiFile})
            .orElse(null);
        if (contentTargets != null) {
            return contentTargets;
        }

        PsiElement[] pageTargets = OctoberPageFilterTarget.find(documentText, offset)
            .flatMap(match -> resolvePage(sourceElement, match.pageName()))
            .map(psiFile -> new PsiElement[]{psiFile})
            .orElse(null);
        if (pageTargets != null) {
            return pageTargets;
        }

        return OctoberComponentBlockParser.find(documentText, offset)
            .flatMap(match -> resolveComponent(sourceElement, match.alias()))
            .map(psiFile -> new PsiElement[]{psiFile})
            .orElse(null);
    }

    private static java.util.Optional<PsiFile> resolvePartial(PsiElement sourceElement, String partialName) {
        Project project = sourceElement.getProject();
        VirtualFile currentFile = findCurrentVirtualFile(sourceElement);
        if (currentFile == null) {
            return java.util.Optional.empty();
        }

        return OctoberThemePartialResolver.findPartial(Path.of(currentFile.getPath()), partialName)
            .map(LocalFileSystem.getInstance()::findFileByNioFile)
            .map(virtualFile -> PsiManager.getInstance(project).findFile(virtualFile));
    }

    private static java.util.Optional<PsiFile> resolveLayout(PsiElement sourceElement, String layoutName) {
        Project project = sourceElement.getProject();
        VirtualFile currentFile = findCurrentVirtualFile(sourceElement);
        if (currentFile == null) {
            return java.util.Optional.empty();
        }

        return OctoberThemeLayoutResolver.findLayout(Path.of(currentFile.getPath()), layoutName)
            .map(LocalFileSystem.getInstance()::findFileByNioFile)
            .map(virtualFile -> PsiManager.getInstance(project).findFile(virtualFile));
    }

    private static java.util.Optional<PsiFile> resolveContent(PsiElement sourceElement, String contentName) {
        Project project = sourceElement.getProject();
        VirtualFile currentFile = findCurrentVirtualFile(sourceElement);
        if (currentFile == null) {
            return java.util.Optional.empty();
        }

        return OctoberThemeContentResolver.findContent(Path.of(currentFile.getPath()), contentName)
            .map(LocalFileSystem.getInstance()::findFileByNioFile)
            .map(virtualFile -> PsiManager.getInstance(project).findFile(virtualFile));
    }

    private static java.util.Optional<PsiFile> resolvePage(PsiElement sourceElement, String pageName) {
        Project project = sourceElement.getProject();
        VirtualFile currentFile = findCurrentVirtualFile(sourceElement);
        if (currentFile == null) {
            return java.util.Optional.empty();
        }

        return OctoberThemePageResolver.findPage(Path.of(currentFile.getPath()), pageName)
            .map(LocalFileSystem.getInstance()::findFileByNioFile)
            .map(virtualFile -> PsiManager.getInstance(project).findFile(virtualFile));
    }

    private static java.util.Optional<PsiFile> resolveComponent(PsiElement sourceElement, String alias) {
        Project project = sourceElement.getProject();
        VirtualFile currentFile = findCurrentVirtualFile(sourceElement);
        if (currentFile == null) {
            return java.util.Optional.empty();
        }

        return OctoberComponentResolver.findComponent(Path.of(currentFile.getPath()), alias)
            .map(LocalFileSystem.getInstance()::findFileByNioFile)
            .map(virtualFile -> PsiManager.getInstance(project).findFile(virtualFile));
    }

    private static @Nullable VirtualFile findCurrentVirtualFile(PsiElement element) {
        PsiFile topLevelFile = InjectedLanguageManager.getInstance(element.getProject()).getTopLevelFile(element);
        if (topLevelFile != null && topLevelFile.getVirtualFile() != null) {
            return topLevelFile.getVirtualFile();
        }

        PsiFile containingFile = element.getContainingFile();
        return containingFile == null ? null : containingFile.getVirtualFile();
    }
}
