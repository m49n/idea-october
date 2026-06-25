package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberThemeContentResolverTest {
    @TempDir
    Path projectRoot;

    @Test
    void resolvesContentNameFromCurrentThemeContentRoot() throws IOException {
        Path page = projectRoot.resolve("themes/bcc/pages/journal/list.htm");
        Files.createDirectories(page.getParent());
        Files.writeString(page, "");

        assertEquals(
            projectRoot.resolve("themes/bcc/content/blocks/intro.htm"),
            OctoberThemeContentResolver.resolveContentPath(page, "blocks/intro").orElseThrow()
        );
    }

    @Test
    void keepsExplicitContentExtension() throws IOException {
        Path page = projectRoot.resolve("themes/bcc/pages/journal/list.htm");
        Files.createDirectories(page.getParent());
        Files.writeString(page, "");

        assertEquals(
            projectRoot.resolve("themes/bcc/content/legal/terms.md"),
            OctoberThemeContentResolver.resolveContentPath(page, "legal/terms.md").orElseThrow()
        );
    }

    @Test
    void findsExistingContentFile() throws IOException {
        Path page = projectRoot.resolve("themes/bcc/pages/journal/list.htm");
        Path content = projectRoot.resolve("themes/bcc/content/blocks/intro.htm");
        Files.createDirectories(page.getParent());
        Files.createDirectories(content.getParent());
        Files.writeString(page, "");
        Files.writeString(content, "");

        assertEquals(content, OctoberThemeContentResolver.findContent(page, "blocks/intro").orElseThrow());
    }

    @Test
    void listsThemeContentNamesRecursively() throws IOException {
        Path page = projectRoot.resolve("themes/bcc/pages/journal/list.htm");
        Path first = projectRoot.resolve("themes/bcc/content/blocks/intro.htm");
        Path second = projectRoot.resolve("themes/bcc/content/legal/terms.md");
        Files.createDirectories(page.getParent());
        Files.createDirectories(first.getParent());
        Files.createDirectories(second.getParent());
        Files.writeString(page, "");
        Files.writeString(first, "");
        Files.writeString(second, "");

        assertEquals(
            List.of("blocks/intro.htm", "legal/terms.md"),
            OctoberThemeContentResolver.listContentNames(page)
        );
    }

    @Test
    void rejectsContentPathTraversal() throws IOException {
        Path page = projectRoot.resolve("themes/bcc/pages/journal/list.htm");
        Files.createDirectories(page.getParent());
        Files.writeString(page, "");

        assertTrue(OctoberThemeContentResolver.resolveContentPath(page, "../secret").isEmpty());
    }
}
