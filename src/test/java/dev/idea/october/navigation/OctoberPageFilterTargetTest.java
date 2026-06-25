package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberPageFilterTargetTest {
    @Test
    void findsPageNameWhenCaretIsInsidePageFilterString() {
        String text = "<a href=\"{{ 'about/press-center-new/post'|page({slug: topPost.slug}) }}/\"></a>";
        int caretOffset = text.indexOf("press-center");

        OctoberPageFilterTarget.Match match = OctoberPageFilterTarget.find(text, caretOffset).orElseThrow();

        assertEquals("about/press-center-new/post", match.pageName());
        assertEquals(
            TextRange.create(text.indexOf("about/press-center-new/post"), text.indexOf("about/press-center-new/post") + 27),
            match.rangeInFile()
        );
    }

    @Test
    void ignoresPlainStringWithoutPageFilter() {
        String text = "{{ 'about/press-center-new/post'|theme }}";
        int caretOffset = text.indexOf("press-center");

        assertTrue(OctoberPageFilterTarget.find(text, caretOffset).isEmpty());
    }

    @Test
    void findsPageNameWhenCaretIsInsidePageUrlFunctionString() {
        String text = "{{ pageUrl('about/press-center-new/post', { slug: topPost.slug }) }}";
        int caretOffset = text.indexOf("press-center");

        OctoberPageFilterTarget.Match match = OctoberPageFilterTarget.find(text, caretOffset).orElseThrow();

        assertEquals("about/press-center-new/post", match.pageName());
        assertEquals(
            TextRange.create(text.indexOf("about/press-center-new/post"), text.indexOf("about/press-center-new/post") + 27),
            match.rangeInFile()
        );
    }
}
