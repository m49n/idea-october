package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class OctoberTwigTagCompletion {
    private static final List<Tag> TAGS = List.of(
        quotedArgument("partial"),
        quotedArgument("ajaxPartial"),
        quotedArgument("component"),
        quotedArgument("content"),
        simple("page"),
        simple("framework"),
        simple("meta"),
        simple("styles"),
        simple("scripts"),
        simple("flash"),
        simple("default"),
        simple("verbatim"),
        spacedArgument("placeholder"),
        spacedArgument("put"),
        spacedArgument("props"),
        spacedArgument("cache")
    );

    private OctoberTwigTagCompletion() {
    }

    public static @NotNull List<Tag> tags() {
        return TAGS;
    }

    private static Tag quotedArgument(String name) {
        return new Tag(name, "October tag", " \"\"", 2);
    }

    private static Tag spacedArgument(String name) {
        return new Tag(name, "October tag", " ", 1);
    }

    private static Tag simple(String name) {
        return new Tag(name, "October tag", "", 0);
    }

    public record Tag(
        @NotNull String name,
        @NotNull String typeText,
        @NotNull String tailText,
        int caretShift
    ) {
    }
}
