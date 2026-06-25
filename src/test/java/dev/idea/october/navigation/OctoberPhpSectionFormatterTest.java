package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OctoberPhpSectionFormatterTest {
    @Test
    void formatsPhpSectionIndentation() {
        String source = """
            function onStart()
            {
            if ($this->param('category')) {
            $this['category'] = $this->param('category');
            } else {
            $this['category'] = null;
            }
            }
            """.stripTrailing();

        assertEquals(
            """
                function onStart()
                {
                    if ($this->param('category')) {
                        $this['category'] = $this->param('category');
                    } else {
                        $this['category'] = null;
                    }
                }
                """.stripTrailing(),
            OctoberPhpSectionFormatter.format(source)
        );
    }

    @Test
    void preservesBlankLines() {
        String source = """
            function onStart()
            {

            $this['category'] = null;
            }
            """.stripTrailing();

        assertEquals(
            """
                function onStart()
                {

                    $this['category'] = null;
                }
                """.stripTrailing(),
            OctoberPhpSectionFormatter.format(source)
        );
    }
}
