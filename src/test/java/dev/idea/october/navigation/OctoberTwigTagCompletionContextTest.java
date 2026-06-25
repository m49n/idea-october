package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberTwigTagCompletionContextTest {
    @Test
    void findsPrefixInsideOpeningTwigTagName() {
        String text = "{% par %}";
        int caretOffset = text.indexOf("par") + 3;

        OctoberTwigTagCompletionContext.Context context = OctoberTwigTagCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("par", context.prefix());
    }

    @Test
    void findsEmptyPrefixAfterOpeningTwigTag() {
        String text = "{%  %}";
        int caretOffset = text.indexOf("  %}") + 2;

        OctoberTwigTagCompletionContext.Context context = OctoberTwigTagCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("", context.prefix());
    }

    @Test
    void ignoresCaretAfterTagNameArgumentsStarted() {
        String text = "{% partial \"journal/list\" %}";
        int caretOffset = text.indexOf("\"journal");

        assertTrue(OctoberTwigTagCompletionContext.find(text, caretOffset).isEmpty());
    }

    @Test
    void removesIntellijCompletionDummyIdentifierFromPrefix() {
        String text = "{% parIntellijIdeaRulezzz %}";
        int caretOffset = text.indexOf("IntellijIdeaRulezzz") + "IntellijIdeaRulezzz".length();

        OctoberTwigTagCompletionContext.Context context = OctoberTwigTagCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("par", context.prefix());
    }
}
