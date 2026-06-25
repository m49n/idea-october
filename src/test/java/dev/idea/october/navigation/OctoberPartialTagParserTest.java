package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OctoberPartialTagParserTest {
    @Test
    void extractsPartialReferenceFromWholeTwigTagText() {
        String tag = "{% partial \"journal/list-category\" category=category list=categories %}";

        List<OctoberPartialTagParser.Match> matches = OctoberPartialTagParser.findMatches(
            tag,
            tag,
            0,
            tag.length(),
            false
        );

        assertEquals(1, matches.size());
        assertEquals("journal/list-category", matches.get(0).partialName());
        assertEquals(TextRange.create(12, 33), matches.get(0).rangeInElement());
    }

    @Test
    void extractsPartialReferenceFromAjaxPartialTagText() {
        String tag = "{% ajaxPartial 'counter' lazy %}";

        List<OctoberPartialTagParser.Match> matches = OctoberPartialTagParser.findMatches(
            tag,
            tag,
            0,
            tag.length(),
            false
        );

        assertEquals(1, matches.size());
        assertEquals("counter", matches.get(0).partialName());
        assertEquals(TextRange.create(16, 23), matches.get(0).rangeInElement());
    }

    @Test
    void extractsPartialReferenceFromQuotedStringLeaf() {
        String fileText = "{% partial \"journal/list-category\" category=category list=categories %}";
        String elementText = "\"journal/list-category\"";
        int startOffset = fileText.indexOf(elementText);

        List<OctoberPartialTagParser.Match> matches = OctoberPartialTagParser.findMatches(
            elementText,
            fileText,
            startOffset,
            startOffset + elementText.length(),
            true
        );

        assertEquals(1, matches.size());
        assertEquals("journal/list-category", matches.get(0).partialName());
        assertEquals(TextRange.create(1, 22), matches.get(0).rangeInElement());
    }

    @Test
    void extractsAjaxPartialReferenceFromQuotedStringLeaf() {
        String fileText = "{% ajaxPartial 'counter' lazy %}";
        String elementText = "'counter'";
        int startOffset = fileText.indexOf(elementText);

        List<OctoberPartialTagParser.Match> matches = OctoberPartialTagParser.findMatches(
            elementText,
            fileText,
            startOffset,
            startOffset + elementText.length(),
            true
        );

        assertEquals(1, matches.size());
        assertEquals("counter", matches.get(0).partialName());
        assertEquals(TextRange.create(1, 8), matches.get(0).rangeInElement());
    }
}
