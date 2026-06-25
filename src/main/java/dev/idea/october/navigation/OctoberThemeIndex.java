package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class OctoberThemeIndex {
    private final Path projectRoot;
    private final String name;
    private final Path themeRoot;
    private final Map<OctoberThemeFileType, List<OctoberThemeFile>> files;

    private OctoberThemeIndex(
        @NotNull Path projectRoot,
        @NotNull String name,
        @NotNull Path themeRoot,
        @NotNull Map<OctoberThemeFileType, List<OctoberThemeFile>> files
    ) {
        this.projectRoot = projectRoot;
        this.name = name;
        this.themeRoot = themeRoot;
        this.files = files;
    }

    public static @NotNull OctoberThemeIndex fromThemeRoot(@NotNull Path projectRoot, @NotNull Path themeRoot) {
        Map<OctoberThemeFileType, List<OctoberThemeFile>> files = new EnumMap<>(OctoberThemeFileType.class);
        for (OctoberThemeFileType type : OctoberThemeFileType.values()) {
            files.put(type, scanThemeFiles(themeRoot, type));
        }

        return new OctoberThemeIndex(
            projectRoot.toAbsolutePath().normalize(),
            themeRoot.getFileName().toString(),
            themeRoot.toAbsolutePath().normalize(),
            files
        );
    }

    public @NotNull Path projectRoot() {
        return projectRoot;
    }

    public @NotNull String name() {
        return name;
    }

    public @NotNull Path themeRoot() {
        return themeRoot;
    }

    public @NotNull List<String> listNames(@NotNull OctoberThemeFileType type) {
        return files.getOrDefault(type, List.of()).stream()
            .map(OctoberThemeFile::name)
            .sorted(Comparator.naturalOrder())
            .toList();
    }

    public @NotNull Optional<Path> resolvePath(@NotNull OctoberThemeFileType type, String fileName) {
        return type.toRelativePath(fileName)
            .map(relativePath -> themeRoot.resolve(type.directoryName()).resolve(relativePath).normalize())
            .filter(path -> path.startsWith(themeRoot.resolve(type.directoryName()).normalize()));
    }

    public @NotNull Optional<Path> findPath(@NotNull OctoberThemeFileType type, String fileName) {
        return resolvePath(type, fileName).filter(Files::isRegularFile);
    }

    public boolean contains(@NotNull Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        return normalized.equals(themeRoot) || normalized.startsWith(themeRoot);
    }

    private static List<OctoberThemeFile> scanThemeFiles(Path themeRoot, OctoberThemeFileType type) {
        Path typeRoot = themeRoot.resolve(type.directoryName()).normalize();
        if (!Files.isDirectory(typeRoot)) {
            return List.of();
        }

        try (Stream<Path> paths = Files.walk(typeRoot)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(type::acceptsFile)
                .map(path -> toThemeFile(typeRoot, type, path))
                .sorted(Comparator.comparing(OctoberThemeFile::name))
                .toList();
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private static OctoberThemeFile toThemeFile(Path typeRoot, OctoberThemeFileType type, Path path) {
        Path relativePath = typeRoot.relativize(path);
        return new OctoberThemeFile(type, type.indexedName(relativePath), path);
    }
}
