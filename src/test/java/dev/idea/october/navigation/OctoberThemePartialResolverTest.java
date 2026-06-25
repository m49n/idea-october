package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberThemePartialResolverTest {
    @TempDir
    Path projectRoot;

    @Test
    void resolvesPartialNameFromCurrentThemePartialsRoot() throws IOException {
        Path page = projectRoot.resolve("themes/bcc/pages/journal/list.htm");
        Files.createDirectories(page.getParent());
        Files.writeString(page, "{% partial \"journal/list-category\" category=category list=categories %}");

        Path expected = projectRoot.resolve("themes/bcc/partials/journal/list-category.htm");

        assertEquals(
            expected,
            OctoberThemePartialResolver.resolvePartialPath(page, "journal/list-category").orElseThrow()
        );
    }

    @Test
    void findsExistingPartialFile() throws IOException {
        Path page = projectRoot.resolve("themes/bcc/pages/journal/list.htm");
        Path partial = projectRoot.resolve("themes/bcc/partials/journal/list-category.htm");
        Files.createDirectories(page.getParent());
        Files.createDirectories(partial.getParent());
        Files.writeString(page, "");
        Files.writeString(partial, "");

        assertEquals(partial, OctoberThemePartialResolver.findPartial(page, "journal/list-category").orElseThrow());
    }

    @Test
    void rejectsPartialPathTraversal() throws IOException {
        Path page = projectRoot.resolve("themes/bcc/pages/journal/list.htm");
        Files.createDirectories(page.getParent());
        Files.writeString(page, "");

        assertTrue(OctoberThemePartialResolver.resolvePartialPath(page, "../secret").isEmpty());
    }

    @Test
    void listsThemePartialNamesRecursivelyWithoutExtension() throws IOException {
        Path page = projectRoot.resolve("themes/bcc/pages/journal/list.htm");
        Path first = projectRoot.resolve("themes/bcc/partials/journal/list-category.htm");
        Path second = projectRoot.resolve("themes/bcc/partials/header.htm");
        Path ignored = projectRoot.resolve("themes/bcc/partials/readme.txt");
        Files.createDirectories(page.getParent());
        Files.createDirectories(first.getParent());
        Files.writeString(page, "");
        Files.writeString(first, "");
        Files.writeString(second, "");
        Files.writeString(ignored, "");

        assertEquals(
            List.of("header", "journal/list-category"),
            OctoberThemePartialResolver.listPartialNames(page)
        );
    }
}
