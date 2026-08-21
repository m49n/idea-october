package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberTailorComponentTargetTest {
    private static final String PAGE = """
        url = "/blog"

        [collection posts]
        handle = "Blog\\Post"
        recordsPerPage = 10
        ==
        {% for post in posts %}
        {% endfor %}
        """;

    @Test
    void findsBlueprintHandleWhenCaretIsOnTailorComponentType() {
        OctoberTailorComponentTarget.Match match = OctoberTailorComponentTarget
            .find(PAGE, PAGE.indexOf("collection") + 2)
            .orElseThrow();

        assertEquals("Blog\\Post", match.handle());
        assertEquals("Blog\\Post", match.rangeInFile().substring(PAGE));
    }

    @Test
    void findsBlueprintHandleWhenCaretIsOnTailorPageAlias() {
        OctoberTailorComponentTarget.Match match = OctoberTailorComponentTarget
            .find(PAGE, PAGE.indexOf("posts") + 2)
            .orElseThrow();

        assertEquals("Blog\\Post", match.handle());
    }

    @Test
    void findsBlueprintHandleWhenCaretIsInsideHandleValue() {
        int handleOffset = PAGE.indexOf("Blog\\Post");

        OctoberTailorComponentTarget.Match match = OctoberTailorComponentTarget
            .find(PAGE, handleOffset + 3)
            .orElseThrow();

        assertEquals("Blog\\Post", match.handle());
        assertEquals(TextRange.create(handleOffset, handleOffset + "Blog\\Post".length()), match.rangeInFile());
    }

    @Test
    void findsUnquotedBlueprintHandleUsedByOctoberCms() {
        String text = """
            [section portfolio]
            handle = Content\\Portfolios

            [collection portfolios]
            handle = Content\\Portfolios
            ==
            """;
        int handleOffset = text.indexOf("Content\\Portfolios");

        OctoberTailorComponentTarget.Match match = OctoberTailorComponentTarget
            .find(text, text.indexOf("section") + 2)
            .orElseThrow();

        assertEquals("Content\\Portfolios", match.handle());
        assertEquals(
            TextRange.create(handleOffset, handleOffset + "Content\\Portfolios".length()),
            match.rangeInFile()
        );
    }

    @Test
    void supportsSectionAndGlobalComponents() {
        String text = """
            [section post]
            handle = 'Blog\\Post'

            [global config]
            handle = "Site\\Config"
            ==
            """;

        assertEquals(
            "Blog\\Post",
            OctoberTailorComponentTarget.find(text, text.indexOf("section") + 2).orElseThrow().handle()
        );
        assertEquals(
            "Site\\Config",
            OctoberTailorComponentTarget.find(text, text.indexOf("config") + 2).orElseThrow().handle()
        );
    }

    @Test
    void ignoresHandlePropertyOnRegularComponents() {
        String text = "[Catalog]\nhandle = \"Blog\\Post\"\n==";

        assertTrue(OctoberTailorComponentTarget.find(text, text.indexOf("Blog\\Post")).isEmpty());
    }
}
