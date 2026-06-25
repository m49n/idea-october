package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberThemeTemplatePathTest {
    @Test
    void acceptsThemePagesLayoutsAndPartials() {
        assertTrue(OctoberThemeTemplatePath.isThemeTemplate(
            Path.of("D:/site/themes/bcc/pages/journal/list.htm")
        ));
        assertTrue(OctoberThemeTemplatePath.isThemeTemplate(
            Path.of("D:/site/themes/bcc/layouts/main.htm")
        ));
        assertTrue(OctoberThemeTemplatePath.isThemeTemplate(
            Path.of("D:/site/themes/bcc/partials/journal/list-category.htm")
        ));
    }

    @Test
    void rejectsFilesOutsideThemeTemplates() {
        assertFalse(OctoberThemeTemplatePath.isThemeTemplate(
            Path.of("D:/site/plugins/webinsane/bcc/components/PressCenterPostsComponent.php")
        ));
        assertFalse(OctoberThemeTemplatePath.isThemeTemplate(
            Path.of("D:/site/themes/bcc/assets/app.htm")
        ));
        assertFalse(OctoberThemeTemplatePath.isThemeTemplate(
            Path.of("D:/site/themes/bcc/pages/journal/list.twig")
        ));
    }
}
