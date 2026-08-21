package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OctoberTwigCompletionCatalogTest {
    @Test
    void exposesOctoberFilters() {
        assertEquals(
            List.of("app", "page", "link", "theme", "trans", "media", "resize", "default", "raw", "md", "currency"),
            OctoberTwigCompletionCatalog.filters()
        );
    }

    @Test
    void exposesOctoberThisProperties() {
        assertEquals(
            List.of("page", "layout", "theme", "param", "controller", "environment", "session", "request", "site"),
            OctoberTwigCompletionCatalog.thisProperties()
        );
    }
}
