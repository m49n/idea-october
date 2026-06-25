package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberComponentCompletionContextTest {
    @Test
    void findsPrefixInsideComponentBlock() {
        String text = """
            url = "/journal"
            [Press
            ==
            """;
        int caretOffset = text.indexOf("Press") + 5;

        OctoberComponentCompletionContext.Context context = OctoberComponentCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("Press", context.prefix());
    }

    @Test
    void removesIntellijCompletionDummyIdentifierFromPrefix() {
        String text = """
            [PressIntellijIdeaRulezzz]
            ==
            """;
        int caretOffset = text.indexOf("IntellijIdeaRulezzz") + "IntellijIdeaRulezzz".length();

        OctoberComponentCompletionContext.Context context = OctoberComponentCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("Press", context.prefix());
    }

    @Test
    void ignoresBracketTextAfterMarkupSectionStarts() {
        String text = """
            ==
            [Press
            """;
        int caretOffset = text.indexOf("Press") + 5;

        assertTrue(OctoberComponentCompletionContext.find(text, caretOffset).isEmpty());
    }
}
