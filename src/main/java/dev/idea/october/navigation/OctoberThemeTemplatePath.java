package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public final class OctoberThemeTemplatePath {
    private static final String THEMES_DIR = "themes";
    private static final String TEMPLATE_EXTENSION = ".htm";
    private static final Set<String> TEMPLATE_ROOTS = Set.of("pages", "layouts", "partials");

    private OctoberThemeTemplatePath() {
    }

    public static boolean isThemeTemplate(@NotNull Path path) {
        Path normalized = path.normalize();
        Path fileName = normalized.getFileName();
        if (fileName == null || !fileName.toString().toLowerCase(Locale.ROOT).endsWith(TEMPLATE_EXTENSION)) {
            return false;
        }

        for (int index = 0; index < normalized.getNameCount() - 2; index++) {
            if (!THEMES_DIR.equalsIgnoreCase(normalized.getName(index).toString())) {
                continue;
            }

            String templateRoot = normalized.getName(index + 2).toString().toLowerCase(Locale.ROOT);
            return TEMPLATE_ROOTS.contains(templateRoot);
        }

        return false;
    }
}
