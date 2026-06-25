package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;

public enum OctoberThemeFileType {
    PAGE("pages", ".htm", false),
    LAYOUT("layouts", ".htm", false),
    PARTIAL("partials", ".htm", false),
    CONTENT("content", ".htm", true);

    private final String directoryName;
    private final String defaultExtension;
    private final boolean keepExtensionInIndexedName;

    OctoberThemeFileType(String directoryName, String defaultExtension, boolean keepExtensionInIndexedName) {
        this.directoryName = directoryName;
        this.defaultExtension = defaultExtension;
        this.keepExtensionInIndexedName = keepExtensionInIndexedName;
    }

    public @NotNull String directoryName() {
        return directoryName;
    }

    public boolean acceptsFile(@NotNull Path path) {
        if (this == CONTENT) {
            return true;
        }

        return path.getFileName().toString().endsWith(defaultExtension);
    }

    public @NotNull String indexedName(@NotNull Path relativePath) {
        String value = relativePath.toString().replace('\\', '/');
        if (!keepExtensionInIndexedName && value.endsWith(defaultExtension)) {
            return value.substring(0, value.length() - defaultExtension.length());
        }

        return value;
    }

    public @NotNull Optional<Path> toRelativePath(String name) {
        if (name == null) {
            return Optional.empty();
        }

        String normalized = name.trim().replace('\\', '/');
        if (normalized.isEmpty() || normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*")) {
            return Optional.empty();
        }

        if (this == CONTENT) {
            String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
            if (!fileName.contains(".")) {
                normalized += defaultExtension;
            }
        } else if (!normalized.endsWith(defaultExtension)) {
            normalized += defaultExtension;
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
}
