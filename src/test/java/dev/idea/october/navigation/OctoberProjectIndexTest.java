package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberProjectIndexTest {
    @TempDir
    Path projectRoot;

    @Test
    void findsCurrentProjectAndThemeFromThemeTemplatePath() throws IOException {
        Path page = projectRoot.resolve("themes/bcc/pages/journal/list.htm");
        Files.createDirectories(page.getParent());
        Files.writeString(page, "");

        OctoberProjectIndex index = OctoberProjectIndex.fromPath(page).orElseThrow();
        OctoberThemeIndex theme = index.findThemeForPath(page).orElseThrow();

        assertEquals(projectRoot, index.projectRoot());
        assertEquals("bcc", theme.name());
        assertEquals(projectRoot.resolve("themes/bcc"), theme.themeRoot());
    }

    @Test
    void indexesThemeFilesByTypeAndName() throws IOException {
        Path page = projectRoot.resolve("themes/bcc/pages/journal/list.htm");
        Path layout = projectRoot.resolve("themes/bcc/layouts/main.htm");
        Path partial = projectRoot.resolve("themes/bcc/partials/journal/card.htm");
        Path content = projectRoot.resolve("themes/bcc/content/blocks/intro.htm");
        Path ignored = projectRoot.resolve("themes/bcc/assets/intro.htm");
        Files.createDirectories(page.getParent());
        Files.createDirectories(layout.getParent());
        Files.createDirectories(partial.getParent());
        Files.createDirectories(content.getParent());
        Files.createDirectories(ignored.getParent());
        Files.writeString(page, "");
        Files.writeString(layout, "");
        Files.writeString(partial, "");
        Files.writeString(content, "");
        Files.writeString(ignored, "");

        OctoberThemeIndex theme = OctoberProjectIndex.fromPath(page)
            .orElseThrow()
            .findThemeForPath(page)
            .orElseThrow();

        assertEquals(List.of("journal/list"), theme.listNames(OctoberThemeFileType.PAGE));
        assertEquals(List.of("main"), theme.listNames(OctoberThemeFileType.LAYOUT));
        assertEquals(List.of("journal/card"), theme.listNames(OctoberThemeFileType.PARTIAL));
        assertEquals(List.of("blocks/intro.htm"), theme.listNames(OctoberThemeFileType.CONTENT));
    }

    @Test
    void resolvesMissingThemeFilePathsSafely() throws IOException {
        Path page = projectRoot.resolve("themes/bcc/pages/journal/list.htm");
        Files.createDirectories(page.getParent());
        Files.writeString(page, "");

        OctoberThemeIndex theme = OctoberProjectIndex.fromPath(page)
            .orElseThrow()
            .findThemeForPath(page)
            .orElseThrow();

        assertEquals(
            projectRoot.resolve("themes/bcc/partials/journal/card.htm"),
            theme.resolvePath(OctoberThemeFileType.PARTIAL, "journal/card").orElseThrow()
        );
        assertEquals(
            projectRoot.resolve("themes/bcc/content/blocks/intro.htm"),
            theme.resolvePath(OctoberThemeFileType.CONTENT, "blocks/intro").orElseThrow()
        );
        assertTrue(theme.resolvePath(OctoberThemeFileType.PARTIAL, "../secret").isEmpty());
    }
}
