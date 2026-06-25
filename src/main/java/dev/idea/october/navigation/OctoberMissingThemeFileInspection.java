package dev.idea.october.navigation;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class OctoberMissingThemeFileInspection extends LocalInspectionTool {
    @Override
    public @NotNull String getDisplayName() {
        return "Missing October CMS theme file";
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(
        @NotNull ProblemsHolder holder,
        boolean isOnTheFly
    ) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                inspectFile(file, holder);
            }
        };
    }

    private static void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        VirtualFile currentFile = file.getVirtualFile();
        if (currentFile == null || findThemeRoot(currentFile).isEmpty()) {
            return;
        }

        for (OctoberThemeReferenceScanner.Reference reference : OctoberThemeReferenceScanner.scan(file.getText())) {
            if (exists(file, currentFile, reference)) {
                continue;
            }

            holder.registerProblem(
                file,
                problemMessage(reference),
                ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                reference.rangeInFile(),
                new CreateThemeFileQuickFix(reference.type(), reference.name())
            );
        }
    }

    private static boolean exists(
        @NotNull PsiFile psiFile,
        @NotNull VirtualFile currentFile,
        @NotNull OctoberThemeReferenceScanner.Reference reference
    ) {
        return findThemeFile(currentFile, reference.type(), reference.name()).isPresent()
            || findThemeFileOnDisk(psiFile, reference.type(), reference.name()).isPresent();
    }

    private static @NotNull Optional<VirtualFile> findThemeFile(
        @NotNull VirtualFile currentFile,
        @NotNull OctoberThemeFileType type,
        @NotNull String name
    ) {
        Optional<Path> relativePath = type.toRelativePath(name);
        if (relativePath.isEmpty()) {
            return Optional.empty();
        }

        return findThemeRoot(currentFile)
            .map(themeRoot -> themeRoot.findChild(type.directoryName()))
            .map(typeRoot -> typeRoot.findFileByRelativePath(toVirtualRelativePath(relativePath.get())));
    }

    private static @NotNull Optional<Path> findThemeFileOnDisk(
        @NotNull PsiFile psiFile,
        @NotNull OctoberThemeFileType type,
        @NotNull String name
    ) {
        VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile == null) {
            return Optional.empty();
        }

        Path currentPath = Path.of(virtualFile.getPath());
        return switch (type) {
            case PAGE -> OctoberThemePageResolver.findPage(currentPath, name);
            case LAYOUT -> OctoberThemeLayoutResolver.findLayout(currentPath, name);
            case PARTIAL -> OctoberThemePartialResolver.findPartial(currentPath, name);
            case CONTENT -> OctoberThemeContentResolver.findContent(currentPath, name);
        };
    }

    private static @NotNull Optional<Path> resolveThemeFilePath(
        @NotNull PsiFile psiFile,
        @NotNull OctoberThemeFileType type,
        @NotNull String name
    ) {
        VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile == null) {
            return Optional.empty();
        }

        Path currentPath = Path.of(virtualFile.getPath());
        return switch (type) {
            case PAGE -> OctoberThemePageResolver.resolvePagePath(currentPath, name);
            case LAYOUT -> OctoberThemeLayoutResolver.resolveLayoutPath(currentPath, name);
            case PARTIAL -> OctoberThemePartialResolver.resolvePartialPath(currentPath, name);
            case CONTENT -> OctoberThemeContentResolver.resolveContentPath(currentPath, name);
        };
    }

    private static @NotNull Optional<VirtualFile> findThemeRoot(@NotNull VirtualFile currentFile) {
        VirtualFile current = currentFile.isDirectory() ? currentFile : currentFile.getParent();
        while (current != null) {
            VirtualFile parent = current.getParent();
            if (parent != null && "themes".equalsIgnoreCase(parent.getName())) {
                return Optional.of(current);
            }

            current = parent;
        }

        return Optional.empty();
    }

    private static String problemMessage(@NotNull OctoberThemeReferenceScanner.Reference reference) {
        return "October " + typeLabel(reference.type()) + " '" + reference.name() + "' not found";
    }

    private static String quickFixName(@NotNull OctoberThemeFileType type, @NotNull String name) {
        return "Create October " + typeLabel(type) + " '" + name + "'";
    }

    private static String typeLabel(@NotNull OctoberThemeFileType type) {
        return switch (type) {
            case PAGE -> "page";
            case LAYOUT -> "layout";
            case PARTIAL -> "partial";
            case CONTENT -> "content";
        };
    }

    private static String toVirtualRelativePath(@NotNull Path relativePath) {
        return relativePath.toString().replace('\\', '/');
    }

    private record CreateThemeFileQuickFix(
        @NotNull OctoberThemeFileType type,
        @NotNull String name
    ) implements LocalQuickFix {
        @Override
        public @NotNull String getName() {
            return quickFixName(type, name);
        }

        @Override
        public @NotNull String getFamilyName() {
            return "Create missing October CMS theme file";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiFile psiFile = descriptor.getPsiElement().getContainingFile();
            if (psiFile == null) {
                return;
            }

            VirtualFile currentFile = psiFile.getVirtualFile();
            if (currentFile == null) {
                return;
            }

            Optional<Path> relativePath = type.toRelativePath(name);
            if (relativePath.isEmpty()) {
                return;
            }

            ApplicationManager.getApplication().runWriteAction(() -> createThemeFile(
                project,
                psiFile,
                currentFile,
                relativePath.get()
            ));
        }

        private void createThemeFile(
            @NotNull Project project,
            @NotNull PsiFile psiFile,
            @NotNull VirtualFile currentFile,
            @NotNull Path relativePath
        ) {
            try {
                Optional<VirtualFile> createdFromVfs = createThemeFileInVirtualFileTree(currentFile, relativePath);
                if (createdFromVfs.isPresent()) {
                    FileEditorManager.getInstance(project).openFile(createdFromVfs.get(), true);
                    return;
                }

                createThemeFileOnDisk(project, psiFile);
            } catch (IOException ignored) {
                // The inspection remains active; the user can retry after fixing permissions or paths.
            }
        }

        private Optional<VirtualFile> createThemeFileInVirtualFileTree(
            @NotNull VirtualFile currentFile,
            @NotNull Path relativePath
        ) throws IOException {
            Optional<VirtualFile> themeRoot = findThemeRoot(currentFile);
            if (themeRoot.isEmpty()) {
                return Optional.empty();
            }

            VirtualFile typeRoot = findOrCreateDirectory(themeRoot.get(), type.directoryName());
            VirtualFile parent = typeRoot;
            int nameCount = relativePath.getNameCount();
            for (int index = 0; index < nameCount - 1; index++) {
                parent = findOrCreateDirectory(parent, relativePath.getName(index).toString());
            }

            String fileName = relativePath.getFileName().toString();
            VirtualFile file = parent.findChild(fileName);
            if (file == null) {
                file = parent.createChildData(this, fileName);
                VfsUtil.saveText(file, defaultFileText(type));
            }

            return Optional.of(file);
        }

        private void createThemeFileOnDisk(@NotNull Project project, @NotNull PsiFile psiFile) throws IOException {
            Optional<Path> targetPath = resolveThemeFilePath(psiFile, type, name);
            if (targetPath.isEmpty()) {
                return;
            }

            Path path = targetPath.get();
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                Files.writeString(path, defaultFileText(type));
            }

            VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
            if (file != null) {
                FileEditorManager.getInstance(project).openFile(file, true);
            }
        }

        private static VirtualFile findOrCreateDirectory(
            @NotNull VirtualFile parent,
            @NotNull String directoryName
        ) throws IOException {
            VirtualFile child = parent.findChild(directoryName);
            if (child != null) {
                return child;
            }

            return parent.createChildDirectory(CreateThemeFileQuickFix.class, directoryName);
        }

        private static String defaultFileText(@NotNull OctoberThemeFileType type) {
            return type == OctoberThemeFileType.PAGE ? "url = \"\"\n==\n" : "";
        }
    }
}
