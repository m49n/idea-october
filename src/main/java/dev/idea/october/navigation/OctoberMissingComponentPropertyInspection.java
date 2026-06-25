package dev.idea.october.navigation;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OctoberMissingComponentPropertyInspection extends LocalInspectionTool {
    @Override
    public @NotNull String getDisplayName() {
        return "Missing October CMS component property";
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

        Map<String, List<OctoberComponentProperty>> propertiesByAlias = new HashMap<>();
        for (OctoberComponentPropertyAssignmentScanner.Assignment assignment :
            OctoberComponentPropertyAssignmentScanner.scan(file.getText())) {
            if (!OctoberComponentAliasProvider.hasComponentAlias(currentFile, assignment.componentAlias())) {
                continue;
            }

            List<OctoberComponentProperty> properties = propertiesByAlias.computeIfAbsent(
                assignment.componentAlias(),
                alias -> OctoberComponentPropertyProvider.listProperties(currentFile, alias)
            );
            if (properties.isEmpty() || hasProperty(properties, assignment.propertyName())) {
                continue;
            }

            holder.registerProblem(
                file,
                problemMessage(assignment, properties),
                ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                assignment.rangeInFile()
            );
        }
    }

    private static boolean hasProperty(List<OctoberComponentProperty> properties, String propertyName) {
        return properties.stream().anyMatch(property -> property.name().equals(propertyName));
    }

    private static String problemMessage(
        OctoberComponentPropertyAssignmentScanner.Assignment assignment,
        List<OctoberComponentProperty> properties
    ) {
        return "October component property '" + assignment.propertyName() + "' not found on '"
            + assignment.componentAlias() + "'. Available: " + availableProperties(properties);
    }

    private static String availableProperties(List<OctoberComponentProperty> properties) {
        return String.join(
            ", ",
            properties.stream()
                .map(OctoberComponentProperty::name)
                .sorted()
                .toList()
        );
    }
}
