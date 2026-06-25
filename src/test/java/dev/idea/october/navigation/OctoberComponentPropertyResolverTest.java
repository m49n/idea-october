package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OctoberComponentPropertyResolverTest {
    @TempDir
    Path projectRoot;

    @Test
    void listsTopLevelPropertiesFromComponentDefineProperties() throws IOException {
        Path page = projectRoot.resolve("themes/bcc/pages/journal/list.htm");
        Path plugin = projectRoot.resolve("plugins/webinsane/bcc/Plugin.php");
        Path component = projectRoot.resolve("plugins/webinsane/bcc/components/PressCenterPostsComponent.php");

        Files.createDirectories(page.getParent());
        Files.createDirectories(plugin.getParent());
        Files.createDirectories(component.getParent());
        Files.writeString(page, "[PressCenterPosts]\n==");
        Files.writeString(plugin, """
            <?php

            namespace Webinsane\\Bcc;

            use Webinsane\\Bcc\\Components\\PressCenterPostsComponent;

            class Plugin
            {
                public function registerComponents()
                {
                    return [
                        PressCenterPostsComponent::class => 'PressCenterPosts',
                    ];
                }
            }
            """);
        Files.writeString(component, """
            <?php

            namespace Webinsane\\Bcc\\Components;

            class PressCenterPostsComponent
            {
                public function defineProperties()
                {
                    return [
                        'slug' => [
                            'title' => 'Slug',
                            'type' => 'string',
                        ],
                        "category" => [
                            "title" => "Category",
                            "type" => "dropdown",
                        ],
                    ];
                }
            }
            """);

        assertEquals(
            List.of("slug", "category"),
            OctoberComponentPropertyResolver.listProperties(page, "PressCenterPosts").stream()
                .map(OctoberComponentProperty::name)
                .toList()
        );
    }

    @Test
    void readsStringAndRawDefaultValuesFromComponentDefineProperties() {
        List<OctoberComponentProperty> properties = OctoberComponentPropertyResolver.listPropertiesFromSource("""
            <?php

            class PressCenterPostsComponent
            {
                public function defineProperties()
                {
                    return [
                        'slug' => [
                            'title' => 'Slug',
                            'default' => '{{ :slug }}',
                            'type' => 'string',
                        ],
                        'enabled' => [
                            'title' => 'Enabled',
                            'default' => 1,
                            'type' => 'checkbox',
                        ],
                    ];
                }
            }
            """);

        assertEquals("slug", properties.get(0).name());
        assertEquals("{{ :slug }}", properties.get(0).defaultValue());
        assertTrue(properties.get(0).quotedDefaultValue());
        assertEquals("enabled", properties.get(1).name());
        assertEquals("1", properties.get(1).defaultValue());
        assertFalse(properties.get(1).quotedDefaultValue());
    }
}
