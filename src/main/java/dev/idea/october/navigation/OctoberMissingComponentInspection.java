package dev.idea.october.navigation;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public final class OctoberMissingComponentInspection extends LocalInspectionTool {
    @Override
    public @NotNull String getDisplayName() {
        return "Missing October CMS component";
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
        if (currentFile == null || !OctoberInspectionFile.shouldInspectThemeTemplate(file)) {
            return;
        }

        for (OctoberComponentBlockParser.Match match : OctoberComponentBlockParser.scan(file.getText())) {
            if (OctoberComponentAliasProvider.hasComponentAlias(currentFile, match.alias())) {
                continue;
            }

            holder.registerProblem(
                file,
                "October component '" + match.alias() + "' not found",
                ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                match.rangeInFile()
            );
        }
    }
}
