package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public record OctoberThemeFile(
    @NotNull OctoberThemeFileType type,
    @NotNull String name,
    @NotNull Path path
) {
}
