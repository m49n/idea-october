package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class OctoberProjectIndex {
    private static final String THEMES_DIR = "themes";

    private final Path projectRoot;
    private final List<OctoberThemeIndex> themes;

    private OctoberProjectIndex(@NotNull Path projectRoot, @NotNull List<OctoberThemeIndex> themes) {
        this.projectRoot = projectRoot;
        this.themes = themes;
    }

    public static @NotNull Optional<OctoberProjectIndex> fromPath(Path path) {
        return findProjectRoot(path)
            .map(projectRoot -> new OctoberProjectIndex(projectRoot, scanThemes(projectRoot)));
    }

    public @NotNull Path projectRoot() {
        return projectRoot;
    }

    public @NotNull List<OctoberThemeIndex> themes() {
        return themes;
    }

    public @NotNull Optional<OctoberThemeIndex> findThemeForPath(Path path) {
        if (path == null) {
            return Optional.empty();
        }

        Path normalized = path.toAbsolutePath().normalize();
        return themes.stream()
            .filter(theme -> theme.contains(normalized))
            .findFirst();
    }

    public @NotNull Optional<Path> resolveThemeFilePath(
        Path currentPath,
        @NotNull OctoberThemeFileType type,
        String fileName
    ) {
        return findThemeForPath(currentPath)
            .flatMap(theme -> theme.resolvePath(type, fileName));
    }

    public @NotNull Optional<Path> findThemeFilePath(
        Path currentPath,
        @NotNull OctoberThemeFileType type,
        String fileName
    ) {
        return findThemeForPath(currentPath)
            .flatMap(theme -> theme.findPath(type, fileName));
    }

    public @NotNull List<String> listThemeFileNames(Path currentPath, @NotNull OctoberThemeFileType type) {
        return findThemeForPath(currentPath)
            .map(theme -> theme.listNames(type))
            .orElse(List.of());
    }

    private static Optional<Path> findProjectRoot(Path path) {
        if (path == null) {
            return Optional.empty();
        }

        Path current = path.toAbsolutePath().normalize();
        Path directory = Files.isDirectory(current) ? current : current.getParent();
        while (directory != null) {
            if (hasFileName(directory, THEMES_DIR)) {
                return Optional.ofNullable(directory.getParent());
            }
            if (Files.isDirectory(directory.resolve(THEMES_DIR))) {
                return Optional.of(directory);
            }
            directory = directory.getParent();
        }

        return Optional.empty();
    }

    private static List<OctoberThemeIndex> scanThemes(Path projectRoot) {
        Path themesRoot = projectRoot.resolve(THEMES_DIR);
        if (!Files.isDirectory(themesRoot)) {
            return List.of();
        }

        try (Stream<Path> themeRoots = Files.list(themesRoot)) {
            return themeRoots
                .filter(Files::isDirectory)
                .map(themeRoot -> OctoberThemeIndex.fromThemeRoot(projectRoot, themeRoot))
                .toList();
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private static boolean hasFileName(Path path, String expectedName) {
        Path fileName = path.getFileName();
        return fileName != null && expectedName.equalsIgnoreCase(fileName.toString());
    }
}
