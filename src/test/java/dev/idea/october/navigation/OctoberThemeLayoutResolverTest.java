package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberThemeLayoutResolverTest {
    @TempDir
    Path projectRoot;

    @Test
    void resolvesLayoutNameFromCurrentThemeLayoutsRoot() throws IOException {
        Path page = projectRoot.resolve("themes/bcc/pages/journal/list.htm");
        Files.createDirectories(page.getParent());
        Files.writeString(page, "");

        assertEquals(
            projectRoot.resolve("themes/bcc/layouts/main.htm"),
            OctoberThemeLayoutResolver.resolveLayoutPath(page, "main").orElseThrow()
        );
    }

    @Test
    void findsExistingLayoutFile() throws IOException {
        Path page = projectRoot.resolve("themes/bcc/pages/journal/list.htm");
        Path layout = projectRoot.resolve("themes/bcc/layouts/main.htm");
        Files.createDirectories(page.getParent());
        Files.createDirectories(layout.getParent());
        Files.writeString(page, "");
        Files.writeString(layout, "");

        assertEquals(layout, OctoberThemeLayoutResolver.findLayout(page, "main").orElseThrow());
    }

    @Test
    void rejectsLayoutPathTraversal() throws IOException {
        Path page = projectRoot.resolve("themes/bcc/pages/journal/list.htm");
        Files.createDirectories(page.getParent());
        Files.writeString(page, "");

        assertTrue(OctoberThemeLayoutResolver.resolveLayoutPath(page, "../secret").isEmpty());
    }

    @Test
    void listsThemeLayoutNames() throws IOException {
        Path page = projectRoot.resolve("themes/bcc/pages/journal/list.htm");
        Path main = projectRoot.resolve("themes/bcc/layouts/main.htm");
        Path landing = projectRoot.resolve("themes/bcc/layouts/landing.htm");
        Files.createDirectories(page.getParent());
        Files.createDirectories(main.getParent());
        Files.writeString(page, "");
        Files.writeString(main, "");
        Files.writeString(landing, "");

        assertEquals(
            List.of("landing", "main"),
            OctoberThemeLayoutResolver.listLayoutNames(page)
        );
    }
}
