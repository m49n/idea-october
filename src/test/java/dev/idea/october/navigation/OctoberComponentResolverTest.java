package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

    @Test
    void listsRegisteredAndConventionalComponentAliases() throws IOException {
        Path page = projectRoot.resolve("themes/bcc/pages/journal/list.htm");
        Path plugin = projectRoot.resolve("plugins/webinsane/bcc/Plugin.php");
        Path registered = projectRoot.resolve("plugins/webinsane/bcc/components/PressCenterPostsComponent.php");
        Path conventional = projectRoot.resolve("plugins/acme/blog/components/LatestPosts.php");

        Files.createDirectories(page.getParent());
        Files.createDirectories(plugin.getParent());
        Files.createDirectories(registered.getParent());
        Files.createDirectories(conventional.getParent());
        Files.writeString(page, "[PressCenterPosts]\n==");
        Files.writeString(registered, "<?php\nclass PressCenterPostsComponent {}\n");
        Files.writeString(conventional, "<?php\nclass LatestPosts {}\n");
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

        assertEquals(
            List.of("LatestPosts", "PressCenterPosts"),
            OctoberComponentResolver.listComponentAliases(page)
        );
    }

    @Test
    void ignoresPluginMetadataAndPermissionStringPairsWhenListingComponentAliases() throws IOException {
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

            use Webinsane\\Bcc\\Components\\PressCenterPostsComponent;

            class Plugin
            {
                public function pluginDetails()
                {
                    return [
                        'icon' => '/plugins/vdlp/redirect/assets/images/icon.svg',
                        'homepage' => 'https://octobercms.com/plugin/vdlp-redirect',
                    ];
                }

                public function registerPermissions()
                {
                    return [
                        'vdlp.redirect.access_redirects' => [
                            'label' => 'vdlp.redirect::lang.permission.access_redirects.label',
                            'tab' => 'vdlp.redirect::lang.permission.access_redirects.tab',
                        ],
                    ];
                }

                public function registerComponents()
                {
                    return [
                        PressCenterPostsComponent::class => 'PressCenterPosts',
                    ];
                }
            }
            """);

        assertEquals(
            List.of("PressCenterPosts"),
            OctoberComponentResolver.listComponentAliases(page)
        );
    }
}
