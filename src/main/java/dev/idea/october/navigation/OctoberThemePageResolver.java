package dev.idea.october.navigation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class OctoberThemePageResolver {
    private static final String THEMES_DIR = "themes";
    private static final String PAGES_DIR = "pages";
    private static final String PAGE_EXTENSION = ".htm";

    private OctoberThemePageResolver() {
    }

    public static Optional<Path> findPage(Path currentTemplatePath, String pageName) {
        return resolvePagePath(currentTemplatePath, pageName)
            .filter(Files::isRegularFile);
    }

    public static Optional<Path> resolvePagePath(Path currentTemplatePath, String pageName) {
        if (currentTemplatePath == null) {
            return Optional.empty();
        }

        Optional<Path> themeRoot = findThemeRoot(currentTemplatePath);
        Optional<Path> relativePage = toRelativePagePath(pageName);
        if (themeRoot.isEmpty() || relativePage.isEmpty()) {
            return Optional.empty();
        }

        Path pagesRoot = themeRoot.get().resolve(PAGES_DIR).normalize();
        Path resolved = pagesRoot.resolve(relativePage.get()).normalize();
        if (!resolved.startsWith(pagesRoot)) {
            return Optional.empty();
        }

        return Optional.of(resolved);
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

    private static Optional<Path> toRelativePagePath(String pageName) {
        if (pageName == null) {
            return Optional.empty();
        }

        String normalized = pageName.trim().replace('\\', '/');
        if (normalized.isEmpty() || normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*")) {
            return Optional.empty();
        }

        if (!normalized.endsWith(PAGE_EXTENSION)) {
            normalized += PAGE_EXTENSION;
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
}
