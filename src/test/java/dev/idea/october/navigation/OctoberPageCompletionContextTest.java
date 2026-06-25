package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberPageCompletionContextTest {
    @Test
    void findsPrefixInsidePageFilterString() {
        String text = "{{ 'about/pr'|page }}";
        int caretOffset = text.indexOf("pr") + 2;

        OctoberPageCompletionContext.Context context = OctoberPageCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("about/pr", context.prefix());
    }

    @Test
    void findsPrefixInsidePageUrlFunctionString() {
        String text = "{{ pageUrl('about/pr') }}";
        int caretOffset = text.indexOf("pr") + 2;

        OctoberPageCompletionContext.Context context = OctoberPageCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("about/pr", context.prefix());
    }

    @Test
    void removesIntellijCompletionDummyIdentifierFromPrefix() {
        String text = "{{ 'about/prIntellijIdeaRulezzz'|page }}";
        int caretOffset = text.indexOf("IntellijIdeaRulezzz") + "IntellijIdeaRulezzz".length();

        OctoberPageCompletionContext.Context context = OctoberPageCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("about/pr", context.prefix());
    }

    @Test
    void ignoresPlainStringWithoutPageContext() {
        String text = "{{ 'about/pr'|theme }}";
        int caretOffset = text.indexOf("pr") + 2;

        assertTrue(OctoberPageCompletionContext.find(text, caretOffset).isEmpty());
    }
}
