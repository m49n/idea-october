package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OctoberComponentResolverTest {
    @TempDir
    Path projectRoot;

    @Test
    void resolvesComponentAliasFromPluginRegistrationUsingClassConstant() throws IOException {
        Path page = projectRoot.resolve("themes/bcc/pages/journal/list.htm");
        Path plugin = projectRoot.resolve("plugins/webinsane/bcc/Plugin.php");
        Path component = projectRoot.resolve("plugins/webinsane/bcc/components/PressCenterPostsComponent.php");

        Files.createDirectories(page.getParent());
        Files.createDirectories(plugin.getParent());
        Files.createDirectories(component.getParent());
        Files.writeString(page, "[PressCenterPosts]\n==");
        Files.writeString(component, "<?php\nclass PressCenterPostsComponent {}\n");
        Files.writeString(plugin, """
            <?php

            namespace Webinsane\\Bcc;

            use System\\Classes\\PluginBase;
            use Webinsane\\Bcc\\Components\\PressCenterPostsComponent;

            class Plugin extends PluginBase
            {
                public function registerComponents()
                {
                    return [
                        PressCenterPostsComponent::class => 'PressCenterPosts',
                    ];
                }
            }
            """);

        assertEquals(component, OctoberComponentResolver.findComponent(page, "PressCenterPosts").orElseThrow());
    }
}
