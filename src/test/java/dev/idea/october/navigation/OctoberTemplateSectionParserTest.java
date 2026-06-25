package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberTemplateSectionParserTest {
    @Test
    void findsPhpSectionBetweenFirstTwoSeparators() {
        String text = """
            url = "/bcc-journal"
            layout = "main"

            [PressCenterPosts]
            slug = "{{:slug}}"
            ==
            function onStart()
            {
                $this['category'] = $this->param('category');
            }
            ==
            {% partial "journal/list-category" %}
            """;

        TextRange phpRange = OctoberTemplateSectionParser.findPhpSection(text).orElseThrow();

        assertEquals(text.indexOf("function onStart()"), phpRange.getStartOffset());
        assertEquals(text.indexOf("\n==\n{% partial"), phpRange.getEndOffset());
        assertEquals("""
            function onStart()
            {
                $this['category'] = $this->param('category');
            }""", phpRange.substring(text));
    }

    @Test
    void ignoresTemplateWithOnlyConfigurationSeparator() {
        String text = """
            url = "/about"
            ==
            <h1>About</h1>
            """;

        assertTrue(OctoberTemplateSectionParser.findPhpSection(text).isEmpty());
    }

    @Test
    void ignoresEmptyPhpSection() {
        String text = """
            url = "/about"
            ==
            ==
            <h1>About</h1>
            """;

        assertTrue(OctoberTemplateSectionParser.findPhpSection(text).isEmpty());
    }

    @Test
    void acceptsWhitespaceAroundSectionSeparators() {
        String text = "url = \"/about\"\n  ==  \nfunction onStart(){}\n == \n<h1>About</h1>";

        TextRange phpRange = OctoberTemplateSectionParser.findPhpSection(text).orElseThrow();

        assertEquals("function onStart(){}", phpRange.substring(text));
    }

    @Test
    void ignoresTwigEqualityOperators() {
        String text = """
            {% if first == second %}
                {{ first == second }}
            {% endif %}
            """;

        assertTrue(OctoberTemplateSectionParser.findPhpSection(text).isEmpty());
    }
}
