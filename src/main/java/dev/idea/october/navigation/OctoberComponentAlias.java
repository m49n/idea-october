package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;

public record OctoberComponentAlias(
    @NotNull String alias,
    @NotNull String owner
) {
}
