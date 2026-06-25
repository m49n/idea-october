package dev.idea.october.navigation;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class OctoberThemePageResolver {
    private OctoberThemePageResolver() {
    }

    public static Optional<Path> findPage(Path currentTemplatePath, String pageName) {
        return OctoberProjectIndex.fromPath(currentTemplatePath)
            .flatMap(index -> index.findThemeFilePath(currentTemplatePath, OctoberThemeFileType.PAGE, pageName));
    }

    public static Optional<Path> resolvePagePath(Path currentTemplatePath, String pageName) {
        return OctoberProjectIndex.fromPath(currentTemplatePath)
            .flatMap(index -> index.resolveThemeFilePath(currentTemplatePath, OctoberThemeFileType.PAGE, pageName));
    }

    public static List<String> listPageNames(Path currentTemplatePath) {
        return OctoberProjectIndex.fromPath(currentTemplatePath)
            .map(index -> index.listThemeFileNames(currentTemplatePath, OctoberThemeFileType.PAGE))
            .orElse(List.of());
    }
}
