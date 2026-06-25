package dev.idea.october.navigation;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class OctoberThemeLayoutResolver {
    private OctoberThemeLayoutResolver() {
    }

    public static Optional<Path> findLayout(Path currentTemplatePath, String layoutName) {
        return OctoberProjectIndex.fromPath(currentTemplatePath)
            .flatMap(index -> index.findThemeFilePath(currentTemplatePath, OctoberThemeFileType.LAYOUT, layoutName));
    }

    public static Optional<Path> resolveLayoutPath(Path currentTemplatePath, String layoutName) {
        return OctoberProjectIndex.fromPath(currentTemplatePath)
            .flatMap(index -> index.resolveThemeFilePath(currentTemplatePath, OctoberThemeFileType.LAYOUT, layoutName));
    }

    public static List<String> listLayoutNames(Path currentTemplatePath) {
        return OctoberProjectIndex.fromPath(currentTemplatePath)
            .flatMap(index -> index.findThemeForPath(currentTemplatePath))
            .map(theme -> theme.listNames(OctoberThemeFileType.LAYOUT))
            .orElseGet(List::of);
    }
}
