package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberContentTargetTest {
    @Test
    void findsContentNameWhenCaretIsInsideContentTagString() {
        String text = "{% content \"blocks/intro.htm\" %}";
        int caretOffset = text.indexOf("intro");

        OctoberContentTarget.Match match = OctoberContentTarget.find(text, caretOffset).orElseThrow();

        assertEquals("blocks/intro.htm", match.contentName());
        assertEquals(TextRange.create(12, 28), match.rangeInFile());
    }

    @Test
    void findsContentNameWhenCaretIsInsideContentFunctionString() {
        String text = "{{ content('blocks/intro') }}";
        int caretOffset = text.indexOf("intro");

        OctoberContentTarget.Match match = OctoberContentTarget.find(text, caretOffset).orElseThrow();

        assertEquals("blocks/intro", match.contentName());
        assertEquals(TextRange.create(12, 24), match.rangeInFile());
    }

    @Test
    void ignoresPlainStringWithoutContentCall() {
        String text = "{{ 'blocks/intro'|theme }}";
        int caretOffset = text.indexOf("intro");

        assertTrue(OctoberContentTarget.find(text, caretOffset).isEmpty());
    }
}
