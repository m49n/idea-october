package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OctoberTwigScopeScannerTest {
    @Test
    void setVariableIsVisibleOnlyBelowItsDeclaration() {
        assertEquals(Set.of(), visibleVariables("{{ some<caret> }}\n{% set someVar = 123 %}"));
        assertEquals(Set.of("someVar"), visibleVariables("{% set someVar = 123 %}\n{{ some<caret> }}"));
    }

    @Test
    void loopVariableIsVisibleOnlyInsideItsLoop() {
        String text = """
            {% for photo in photos %}
                {{ ph<caret> }}
            {% endfor %}
            """;

        assertEquals(Set.of("photo"), visibleVariables(text));
        assertEquals(Set.of(), visibleVariables(text.replace("{{ ph<caret> }}", "{{ photo }}") + "{{ ph<caret> }}"));
    }

    @Test
    void nestedLoopKeepsOuterAndInnerVariablesVisible() {
        String text = """
            {% for album in albums %}
                {% for key, photo in album.photos %}
                    {{ ph<caret> }}
                {% endfor %}
            {% endfor %}
            """;

        assertEquals(Set.of("album", "key", "photo"), visibleVariables(text));
    }

    @Test
    void completedInnerLoopRemovesOnlyItsVariables() {
        String text = """
            {% for album in albums %}
                {% for photo in album.photos %}
                    {{ photo }}
                {% endfor %}
                {{ al<caret> }}
            {% endfor %}
            """;

        assertEquals(Set.of("album"), visibleVariables(text));
    }

    private static Set<String> visibleVariables(String textWithCaret) {
        int caretOffset = textWithCaret.indexOf("<caret>");
        String text = textWithCaret.replace("<caret>", "");
        return OctoberTwigScopeScanner.visibleVariables(text, caretOffset);
    }
}
