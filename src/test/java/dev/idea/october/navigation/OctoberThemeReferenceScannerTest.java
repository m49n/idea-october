package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OctoberThemeReferenceScannerTest {
    @Test
    void scansThemeFileReferences() {
        String text = """
            url = "/bcc-journal"
            layout = "main"
            ==
            {% partial "journal/list-category" %}
            {% ajaxPartial 'counter' %}
            {{ partial('journal/card') }}
            {{ ajaxPartial("modal") }}
            {% content "blocks/intro.htm" %}
            {{ content('legal/terms.md') }}
            {{ 'about/press-center-new/post'|page }}
            {{ pageUrl('journal/post') }}
            """;

        List<OctoberThemeReferenceScanner.Reference> references = OctoberThemeReferenceScanner.scan(text);

        assertEquals(
            List.of(
                reference(OctoberThemeFileType.LAYOUT, "main", text),
                reference(OctoberThemeFileType.PARTIAL, "journal/list-category", text),
                reference(OctoberThemeFileType.PARTIAL, "counter", text),
                reference(OctoberThemeFileType.PARTIAL, "journal/card", text),
                reference(OctoberThemeFileType.PARTIAL, "modal", text),
                reference(OctoberThemeFileType.CONTENT, "blocks/intro.htm", text),
                reference(OctoberThemeFileType.CONTENT, "legal/terms.md", text),
                reference(OctoberThemeFileType.PAGE, "about/press-center-new/post", text),
                reference(OctoberThemeFileType.PAGE, "journal/post", text)
            ),
            references
        );
    }

    private static OctoberThemeReferenceScanner.Reference reference(
        OctoberThemeFileType type,
        String name,
        String text
    ) {
        int start = text.indexOf(name);
        return new OctoberThemeReferenceScanner.Reference(
            type,
            name,
            TextRange.create(start, start + name.length())
        );
    }
}
