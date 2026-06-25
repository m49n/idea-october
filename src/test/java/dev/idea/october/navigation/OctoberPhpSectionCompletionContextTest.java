package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberPhpSectionCompletionContextTest {
    @Test
    void findsPrefixInsidePhpSection() {
        String text = """
            url = "/"
            ==
            function onSt
            ==
            <h1>Page</h1>
            """;
        int caretOffset = text.indexOf("onSt") + "onSt".length();

        OctoberPhpSectionCompletionContext.Context context = OctoberPhpSectionCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("onSt", context.prefix());
    }

    @Test
    void findsDollarThisPrefixInsidePhpSection() {
        String text = """
            url = "/"
            ==
            $thi
            ==
            <h1>Page</h1>
            """;
        int caretOffset = text.indexOf("$thi") + "$thi".length();

        OctoberPhpSectionCompletionContext.Context context = OctoberPhpSectionCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("$thi", context.prefix());
    }

    @Test
    void ignoresMarkupSection() {
        String text = """
            url = "/"
            ==
            function onStart() {}
            ==
            {% partial "journal/list" %}
            """;
        int caretOffset = text.indexOf("partial") + "partial".length();

        assertTrue(OctoberPhpSectionCompletionContext.find(text, caretOffset).isEmpty());
    }
}
