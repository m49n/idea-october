package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberComponentPropertyCompletionContextTest {
    @Test
    void findsPropertyPrefixInsideCurrentComponentBlock() {
        String text = """
            url = "/journal"
            [PressCenterPosts]
            sl
            ==
            """;
        int caretOffset = text.indexOf("sl") + 2;

        OctoberComponentPropertyCompletionContext.Context context = OctoberComponentPropertyCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("PressCenterPosts", context.componentAlias());
        assertEquals("sl", context.prefix());
    }

    @Test
    void usesComponentNameForPropertiesWhenComponentBlockHasPageAlias() {
        String text = """
            url = "/"
            [breadcrumbs breadcrumbsc]
            main
            ==
            """;
        int caretOffset = text.indexOf("main") + 4;

        OctoberComponentPropertyCompletionContext.Context context = OctoberComponentPropertyCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("breadcrumbs", context.componentAlias());
        assertEquals("main", context.prefix());
    }

    @Test
    void allowsHyphenatedPropertyPrefix() {
        String text = """
            [breadcrumbs breadcrumbsc]
            main-ol
            ==
            """;
        int caretOffset = text.indexOf("main-ol") + 7;

        OctoberComponentPropertyCompletionContext.Context context = OctoberComponentPropertyCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("breadcrumbs", context.componentAlias());
        assertEquals("main-ol", context.prefix());
    }

    @Test
    void findsEmptyPropertyPrefixInsideCurrentComponentBlock() {
        String text = """
            url = "/journal"
            [PressCenterPosts]
            
            ==
            """;
        int componentLineEnd = text.indexOf('\n', text.indexOf("[PressCenterPosts]"));
        int caretOffset = componentLineEnd + 1;

        OctoberComponentPropertyCompletionContext.Context context = OctoberComponentPropertyCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("PressCenterPosts", context.componentAlias());
        assertEquals("", context.prefix());
    }

    @Test
    void removesIntellijCompletionDummyIdentifierFromPrefix() {
        String text = """
            [PressCenterPosts]
            slIntellijIdeaRulezzz
            ==
            """;
        int caretOffset = text.indexOf("IntellijIdeaRulezzz") + "IntellijIdeaRulezzz".length();

        OctoberComponentPropertyCompletionContext.Context context = OctoberComponentPropertyCompletionContext
            .find(text, caretOffset)
            .orElseThrow();

        assertEquals("PressCenterPosts", context.componentAlias());
        assertEquals("sl", context.prefix());
    }

    @Test
    void ignoresPropertyValues() {
        String text = """
            [PressCenterPosts]
            slug = "sl"
            ==
            """;
        int caretOffset = text.indexOf("sl\"") + 2;

        assertTrue(OctoberComponentPropertyCompletionContext.find(text, caretOffset).isEmpty());
    }

    @Test
    void ignoresPageSettingsBeforeFirstComponentBlock() {
        String text = """
            url = "/journal"
            ==
            """;
        int caretOffset = text.indexOf("journal") + 7;

        assertTrue(OctoberComponentPropertyCompletionContext.find(text, caretOffset).isEmpty());
    }
}
