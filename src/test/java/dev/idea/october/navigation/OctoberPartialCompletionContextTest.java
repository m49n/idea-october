package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberPartialCompletionContextTest {
    @Test
    void findsPrefixInsidePartialTagString() {
        String text = "{% partial \"journal/li\" category=category %}";
        int caretOffset = text.indexOf("li") + 2;

        OctoberPartialCompletionContext.Context context = OctoberPartialCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("journal/li", context.prefix());
    }

    @Test
    void findsPrefixInsideAjaxPartialTagString() {
        String text = "{% ajaxPartial 'cou' lazy %}";
        int caretOffset = text.indexOf("cou") + 3;

        OctoberPartialCompletionContext.Context context = OctoberPartialCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("cou", context.prefix());
    }

    @Test
    void ignoresCaretAfterPartialStringIsClosed() {
        String text = "{% partial \"journal/list\" category=category %}";
        int caretOffset = text.indexOf(" category");

        assertTrue(OctoberPartialCompletionContext.find(text, caretOffset).isEmpty());
    }

    @Test
    void removesIntellijCompletionDummyIdentifierFromPrefix() {
        String text = "{% partial \"jouIntellijIdeaRulezzz\" %}";
        int caretOffset = text.indexOf("IntellijIdeaRulezzz") + "IntellijIdeaRulezzz".length();

        OctoberPartialCompletionContext.Context context = OctoberPartialCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("jou", context.prefix());
    }
}
