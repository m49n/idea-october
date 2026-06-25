package dev.idea.october.navigation;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class OctoberThemeContentResolver {
    private OctoberThemeContentResolver() {
    }

    public static Optional<Path> findContent(Path currentTemplatePath, String contentName) {
        return OctoberProjectIndex.fromPath(currentTemplatePath)
            .flatMap(index -> index.findThemeFilePath(currentTemplatePath, OctoberThemeFileType.CONTENT, contentName));
    }

    public static Optional<Path> resolveContentPath(Path currentTemplatePath, String contentName) {
        return OctoberProjectIndex.fromPath(currentTemplatePath)
            .flatMap(index -> index.resolveThemeFilePath(currentTemplatePath, OctoberThemeFileType.CONTENT, contentName));
    }

    public static List<String> listContentNames(Path currentTemplatePath) {
        return OctoberProjectIndex.fromPath(currentTemplatePath)
            .map(index -> index.listThemeFileNames(currentTemplatePath, OctoberThemeFileType.CONTENT))
            .orElse(List.of());
    }
}
