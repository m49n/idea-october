package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class OctoberTwigCompletionCatalog {
    private static final List<String> FILTERS = List.of(
        "app",
        "page",
        "link",
        "theme",
        "trans",
        "media",
        "resize",
        "default",
        "raw",
        "md",
        "currency"
    );
    private static final List<String> THIS_PROPERTIES = List.of(
        "page",
        "layout",
        "theme",
        "param",
        "controller",
        "environment",
        "session",
        "request",
        "site"
    );

    private OctoberTwigCompletionCatalog() {
    }

    public static @NotNull List<String> filters() {
        return FILTERS;
    }

    public static @NotNull List<String> thisProperties() {
        return THIS_PROPERTIES;
    }
}
