package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberComponentBlockParserTest {
    @Test
    void findsComponentBlockWhenCaretIsInsideAlias() {
        String text = "url = \"/journal\"\n\n[PressCenterPosts]\ncategory = \"news\"\n==\n<div></div>";
        int caretOffset = text.indexOf("Center");

        OctoberComponentBlockParser.Match match = OctoberComponentBlockParser.find(text, caretOffset).orElseThrow();

        assertEquals("PressCenterPosts", match.alias());
        assertEquals(TextRange.create(19, 35), match.rangeInFile());
    }

    @Test
    void usesComponentNameWhenComponentBlockHasPageAlias() {
        String text = "url = \"/\"\n[breadcrumbs breadcrumbsc]\nmain-ol-class = \"breadcrumbs__list\"\n==";
        int caretOffset = text.indexOf("breadcrumbsc") + 5;

        OctoberComponentBlockParser.Match match = OctoberComponentBlockParser.find(text, caretOffset).orElseThrow();

        assertEquals("breadcrumbs", match.alias());
        assertEquals("breadcrumbs", match.rangeInFile().substring(text));
    }

    @Test
    void scansComponentNameWhenComponentBlockHasPageAlias() {
        String text = "url = \"/\"\n[breadcrumbs breadcrumbsc]\nmain-ol-class = \"breadcrumbs__list\"\n==";

        OctoberComponentBlockParser.Match match = OctoberComponentBlockParser.scan(text).getFirst();

        assertEquals("breadcrumbs", match.alias());
        assertEquals("breadcrumbs", match.rangeInFile().substring(text));
    }

    @Test
    void ignoresBracketTextAfterMarkupSectionStarts() {
        String text = "url = \"/journal\"\n==\n<div>[PressCenterPosts]</div>";
        int caretOffset = text.indexOf("Center");

        assertTrue(OctoberComponentBlockParser.find(text, caretOffset).isEmpty());
    }
}
