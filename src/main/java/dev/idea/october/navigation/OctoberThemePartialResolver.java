package dev.idea.october.navigation;

import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class OctoberThemePartialResolver {
    private static final String THEMES_DIR = "themes";
    private static final String PARTIALS_DIR = "partials";
    private static final String PARTIAL_EXTENSION = ".htm";

    private OctoberThemePartialResolver() {
    }

    public static Optional<Path> findPartial(Path currentTemplatePath, String partialName) {
        return resolvePartialPath(currentTemplatePath, partialName)
            .filter(Files::isRegularFile);
    }

    public static Optional<Path> resolvePartialPath(Path currentTemplatePath, String partialName) {
        if (currentTemplatePath == null) {
            return Optional.empty();
        }

        Optional<Path> themeRoot = findThemeRoot(currentTemplatePath);
        Optional<Path> relativePartial = toRelativePartialPath(partialName);
        if (themeRoot.isEmpty() || relativePartial.isEmpty()) {
            return Optional.empty();
        }

        Path partialsRoot = themeRoot.get().resolve(PARTIALS_DIR).normalize();
        Path resolved = partialsRoot.resolve(relativePartial.get()).normalize();
        if (!resolved.startsWith(partialsRoot)) {
            return Optional.empty();
        }

        return Optional.of(resolved);
    }

    public static List<String> listPartialNames(Path currentTemplatePath) {
        Optional<Path> themeRoot = findThemeRoot(currentTemplatePath);
        if (themeRoot.isEmpty()) {
            return List.of();
        }

        Path partialsRoot = themeRoot.get().resolve(PARTIALS_DIR).normalize();
        if (!Files.isDirectory(partialsRoot)) {
            return List.of();
        }

        try (Stream<Path> partialFiles = Files.walk(partialsRoot)) {
            return partialFiles
                .filter(Files::isRegularFile)
                .filter(OctoberThemePartialResolver::isPartialFile)
                .map(partialsRoot::relativize)
                .map(OctoberThemePartialResolver::toPartialName)
                .sorted(Comparator.naturalOrder())
                .toList();
        }
        catch (IOException ignored) {
            return List.of();
        }
    }

    private static Optional<Path> findThemeRoot(Path currentTemplatePath) {
        Path current = currentTemplatePath.toAbsolutePath().normalize();
        Path directory = Files.isDirectory(current) ? current : current.getParent();

        while (directory != null) {
            Path parent = directory.getParent();
            if (parent != null && hasFileName(parent, THEMES_DIR)) {
                return Optional.of(directory);
            }
            directory = parent;
        }

        return Optional.empty();
    }

    private static Optional<Path> toRelativePartialPath(String partialName) {
        if (partialName == null) {
            return Optional.empty();
        }

        String normalized = partialName.trim().replace('\\', '/');
        if (normalized.isEmpty() || normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*")) {
            return Optional.empty();
        }

        if (!normalized.endsWith(PARTIAL_EXTENSION)) {
            normalized += PARTIAL_EXTENSION;
        }

        Path relativePath = Path.of(normalized).normalize();
        if (relativePath.isAbsolute() || relativePath.startsWith("..")) {
            return Optional.empty();
        }

        for (Path segment : relativePath) {
            String value = segment.toString();
            if (value.isBlank() || ".".equals(value) || "..".equals(value)) {
                return Optional.empty();
            }
        }

        return Optional.of(relativePath);
    }

    private static boolean hasFileName(Path path, String expectedName) {
        Path fileName = path.getFileName();
        return fileName != null && expectedName.equalsIgnoreCase(fileName.toString());
    }

    private static boolean isPartialFile(Path path) {
        return path.getFileName().toString().endsWith(PARTIAL_EXTENSION);
    }

    private static String toPartialName(Path relativePath) {
        String value = relativePath.toString().replace('\\', '/');
        return value.substring(0, value.length() - PARTIAL_EXTENSION.length());
    }
}
