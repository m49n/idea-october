package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberTwigTagCompletionTest {
    @Test
    void partialTagInsertsQuotedArgumentAndPlacesCaretInsideQuotes() {
        OctoberTwigTagCompletion.Tag tag = tagsByName().get("partial");

        assertEquals(" \"\"", tag.tailText());
        assertEquals(2, tag.caretShift());
    }

    @Test
    void ajaxPartialTagInsertsQuotedArgumentAndPlacesCaretInsideQuotes() {
        OctoberTwigTagCompletion.Tag tag = tagsByName().get("ajaxPartial");

        assertEquals(" \"\"", tag.tailText());
        assertEquals(2, tag.caretShift());
    }

    @Test
    void includesCommonOctoberCmsTwigTags() {
        Map<String, OctoberTwigTagCompletion.Tag> tags = tagsByName();

        assertTrue(tags.containsKey("partial"));
        assertTrue(tags.containsKey("ajaxPartial"));
        assertTrue(tags.containsKey("component"));
        assertTrue(tags.containsKey("content"));
        assertTrue(tags.containsKey("page"));
        assertTrue(tags.containsKey("framework"));
    }

    private static Map<String, OctoberTwigTagCompletion.Tag> tagsByName() {
        return OctoberTwigTagCompletion.tags().stream()
            .collect(Collectors.toMap(OctoberTwigTagCompletion.Tag::name, tag -> tag));
    }
}
