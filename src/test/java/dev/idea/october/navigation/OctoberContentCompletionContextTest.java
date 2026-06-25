package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberContentCompletionContextTest {
    @Test
    void findsPrefixInsideContentTagString() {
        String text = "{% content \"blo\" %}";
        int caretOffset = text.indexOf("blo") + 3;

        OctoberContentCompletionContext.Context context = OctoberContentCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("blo", context.prefix());
    }

    @Test
    void findsPrefixInsideContentFunctionString() {
        String text = "{{ content('blo') }}";
        int caretOffset = text.indexOf("blo") + 3;

        OctoberContentCompletionContext.Context context = OctoberContentCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("blo", context.prefix());
    }

    @Test
    void removesIntellijCompletionDummyIdentifierFromPrefix() {
        String text = "{% content \"bloIntellijIdeaRulezzz\" %}";
        int caretOffset = text.indexOf("IntellijIdeaRulezzz") + "IntellijIdeaRulezzz".length();

        OctoberContentCompletionContext.Context context = OctoberContentCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("blo", context.prefix());
    }

    @Test
    void ignoresCaretAfterContentStringIsClosed() {
        String text = "{% content \"blocks/intro\" %}";
        int caretOffset = text.indexOf(" %}");

        assertTrue(OctoberContentCompletionContext.find(text, caretOffset).isEmpty());
    }
}
