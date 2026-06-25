package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberLayoutTargetTest {
    @Test
    void findsLayoutNameInTemplateConfigurationSection() {
        String text = """
            url = "/bcc-journal"
            layout = "main"
            ==
            {% partial "journal/list" %}
            """;
        int caretOffset = text.indexOf("main") + 2;

        OctoberLayoutTarget.Match match = OctoberLayoutTarget.find(text, caretOffset).orElseThrow();

        assertEquals("main", match.layoutName());
        assertEquals(TextRange.create(text.indexOf("main"), text.indexOf("main") + 4), match.rangeInFile());
    }

    @Test
    void ignoresLayoutLikeAssignmentAfterFirstSectionSeparator() {
        String text = """
            url = "/bcc-journal"
            ==
            layout = "main"
            """;
        int caretOffset = text.indexOf("main") + 2;

        assertTrue(OctoberLayoutTarget.find(text, caretOffset).isEmpty());
    }
}
