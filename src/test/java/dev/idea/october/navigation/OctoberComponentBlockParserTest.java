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
    void ignoresBracketTextAfterMarkupSectionStarts() {
        String text = "url = \"/journal\"\n==\n<div>[PressCenterPosts]</div>";
        int caretOffset = text.indexOf("Center");

        assertTrue(OctoberComponentBlockParser.find(text, caretOffset).isEmpty());
    }
}
