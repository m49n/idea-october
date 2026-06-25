package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OctoberPhpSectionIndentationTest {
    @Test
    void indentsLineAfterOpeningBrace() {
        String text = """
            url = "/"
            ==
            function onStart()
            {
            
            }
            ==
            <h1>Page</h1>
            """;
        int lineStartOffset = text.indexOf("\n\n}") + 1;

        assertEquals(4, OctoberPhpSectionIndentation.expectedIndent(text, lineStartOffset));
    }

    @Test
    void unindentsClosingBraceLine() {
        String text = """
            url = "/"
            ==
            function onStart()
            {
                if ($this->param('category')) {
                    $this['category'] = true;
                }
            }
            ==
            <h1>Page</h1>
            """;
        int closingBraceOffset = text.lastIndexOf("}");

        assertEquals(0, OctoberPhpSectionIndentation.expectedIndent(text, closingBraceOffset));
    }

    @Test
    void keepsNestedStatementIndent() {
        String text = """
            url = "/"
            ==
            function onStart()
            {
                if ($this->param('category')) {
                $this['category'] = true;
                }
            }
            ==
            <h1>Page</h1>
            """;
        int assignmentOffset = text.indexOf("$this['category']");

        assertEquals(8, OctoberPhpSectionIndentation.expectedIndent(text, assignmentOffset));
    }
}
