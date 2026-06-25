package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record OctoberComponentProperty(
    @NotNull String name,
    @Nullable String defaultValue,
    boolean quotedDefaultValue
) {
    public OctoberComponentProperty(@NotNull String name) {
        this(name, null, true);
    }
}
