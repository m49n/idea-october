package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberLayoutCompletionContextTest {
    @Test
    void findsPrefixInsideLayoutSettingString() {
        String text = """
            url = "/bcc-journal"
            layout = "ma"
            ==
            {% partial "journal/list" %}
            """;
        int caretOffset = text.indexOf("ma") + 2;

        OctoberLayoutCompletionContext.Context context = OctoberLayoutCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("ma", context.prefix());
    }

    @Test
    void removesIntellijCompletionDummyIdentifierFromPrefix() {
        String text = """
            layout = "maIntellijIdeaRulezzz"
            ==
            """;
        int caretOffset = text.indexOf("IntellijIdeaRulezzz") + "IntellijIdeaRulezzz".length();

        OctoberLayoutCompletionContext.Context context = OctoberLayoutCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("ma", context.prefix());
    }

    @Test
    void ignoresLayoutSettingOutsideConfigurationSection() {
        String text = """
            ==
            layout = "ma"
            """;
        int caretOffset = text.indexOf("ma") + 2;

        assertTrue(OctoberLayoutCompletionContext.find(text, caretOffset).isEmpty());
    }
}
