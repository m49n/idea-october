package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberThemePageResolverTest {
    @TempDir
    Path projectRoot;

    @Test
    void resolvesPageNameFromCurrentThemePagesRoot() throws IOException {
        Path current = projectRoot.resolve("themes/bcc/partials/card.htm");
        Files.createDirectories(current.getParent());
        Files.writeString(current, "");

        Path expected = projectRoot.resolve("themes/bcc/pages/about/press-center-new/post.htm");

        assertEquals(
            expected,
            OctoberThemePageResolver.resolvePagePath(current, "about/press-center-new/post").orElseThrow()
        );
    }

    @Test
    void findsExistingPageFile() throws IOException {
        Path current = projectRoot.resolve("themes/bcc/pages/journal/list.htm");
        Path page = projectRoot.resolve("themes/bcc/pages/about/press-center-new/post.htm");
        Files.createDirectories(current.getParent());
        Files.createDirectories(page.getParent());
        Files.writeString(current, "");
        Files.writeString(page, "");

        assertEquals(page, OctoberThemePageResolver.findPage(current, "about/press-center-new/post").orElseThrow());
    }

    @Test
    void rejectsPagePathTraversal() throws IOException {
        Path current = projectRoot.resolve("themes/bcc/pages/journal/list.htm");
        Files.createDirectories(current.getParent());
        Files.writeString(current, "");

        assertTrue(OctoberThemePageResolver.resolvePagePath(current, "../partials/header").isEmpty());
    }
}
