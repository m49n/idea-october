package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OctoberPartialCaretTargetTest {
    @Test
    void findsPartialNameWhenCaretIsInsidePartialPath() {
        String text = "{% partial \"journal/list-category\" category=category list=categories %}";
        int caretOffset = text.indexOf("category");

        OctoberPartialCaretTarget.Match match = OctoberPartialCaretTarget.find(text, caretOffset).orElseThrow();

        assertEquals("journal/list-category", match.partialName());
        assertEquals(TextRange.create(12, 33), match.rangeInFile());
    }

    @Test
    void findsPartialNameWhenCaretIsInsideAjaxPartialPath() {
        String text = "{% ajaxPartial 'counter' lazy %}";
        int caretOffset = text.indexOf("unt");

        OctoberPartialCaretTarget.Match match = OctoberPartialCaretTarget.find(text, caretOffset).orElseThrow();

        assertEquals("counter", match.partialName());
        assertEquals(TextRange.create(16, 23), match.rangeInFile());
    }
}
