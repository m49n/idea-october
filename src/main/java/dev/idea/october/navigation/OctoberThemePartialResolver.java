package dev.idea.october.navigation;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class OctoberThemePartialResolver {
    private OctoberThemePartialResolver() {
    }

    public static Optional<Path> findPartial(Path currentTemplatePath, String partialName) {
        return OctoberProjectIndex.fromPath(currentTemplatePath)
            .flatMap(index -> index.findThemeFilePath(currentTemplatePath, OctoberThemeFileType.PARTIAL, partialName));
    }

    public static Optional<Path> resolvePartialPath(Path currentTemplatePath, String partialName) {
        return OctoberProjectIndex.fromPath(currentTemplatePath)
            .flatMap(index -> index.resolveThemeFilePath(currentTemplatePath, OctoberThemeFileType.PARTIAL, partialName));
    }

    public static List<String> listPartialNames(Path currentTemplatePath) {
        return OctoberProjectIndex.fromPath(currentTemplatePath)
            .map(index -> index.listThemeFileNames(currentTemplatePath, OctoberThemeFileType.PARTIAL))
            .orElse(List.of());
    }
}
