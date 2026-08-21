package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberTwigExpressionCompletionContextTest {
    @Test
    void findsVariablePrefixInsideOutputExpression() {
        assertContext("{{ some<caret> }}", OctoberTwigExpressionCompletionContext.Kind.VARIABLE, "some");
    }

    @Test
    void findsFilterPrefixAfterPipe() {
        assertContext("{{ value | pa<caret> }}", OctoberTwigExpressionCompletionContext.Kind.FILTER, "pa");
        assertContext("{{ value|<caret> }}", OctoberTwigExpressionCompletionContext.Kind.FILTER, "");
    }

    @Test
    void findsPropertyPrefixAfterThisDot() {
        assertContext("{{ this.pa<caret> }}", OctoberTwigExpressionCompletionContext.Kind.THIS_PROPERTY, "pa");
        assertContext("{{ this.<caret> }}", OctoberTwigExpressionCompletionContext.Kind.THIS_PROPERTY, "");
    }

    @Test
    void supportsExpressionPortionOfTwigStatement() {
        assertContext("{% set result = some<caret> %}", OctoberTwigExpressionCompletionContext.Kind.VARIABLE, "some");
    }

    @Test
    void ignoresPlainTextAndOpeningTagName() {
        assertTrue(find("<p>some<caret></p>").isEmpty());
        assertTrue(find("{% par<caret> %}").isEmpty());
        assertTrue(find("{{ 123<caret> }}").isEmpty());
    }

    @Test
    void removesIntellijCompletionDummyIdentifier() {
        assertContext(
            "{{ someIntellijIdeaRulezzz<caret> }}",
            OctoberTwigExpressionCompletionContext.Kind.VARIABLE,
            "some"
        );
    }

    private static void assertContext(
        String textWithCaret,
        OctoberTwigExpressionCompletionContext.Kind kind,
        String prefix
    ) {
        OctoberTwigExpressionCompletionContext.Context context = find(textWithCaret).orElseThrow();
        assertEquals(kind, context.kind());
        assertEquals(prefix, context.prefix());
    }

    private static java.util.Optional<OctoberTwigExpressionCompletionContext.Context> find(String textWithCaret) {
        int caretOffset = textWithCaret.indexOf("<caret>");
        String text = textWithCaret.replace("<caret>", "");
        return OctoberTwigExpressionCompletionContext.find(text, caretOffset);
    }
}
