package dev.idea.october.navigation;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class OctoberThemeFileCompletionContributorTest extends BasePlatformTestCase {
    public void testCompletesLayoutsInsideLayoutSetting() {
        myFixture.addFileToProject("themes/bcc/layouts/main.htm", "");
        myFixture.addFileToProject("themes/bcc/layouts/landing.htm", "");
        PsiFile page = myFixture.addFileToProject("themes/bcc/pages/journal/list.htm", """
            url = "/bcc-journal"
            layout = "<caret>"
            ==
            {% partial "journal/list" %}
            """);
        myFixture.configureFromExistingVirtualFile(page.getVirtualFile());

        myFixture.completeBasic();

        Set<String> lookupStrings = lookupStrings();
        Path currentPath = Path.of(page.getVirtualFile().getPath());
        assertTrue(
            "lookup=" + lookupStrings
                + ", file=" + currentPath
                + ", layouts=" + OctoberThemeLayoutResolver.listLayoutNames(currentPath),
            lookupStrings.contains("main")
        );
    }

    public void testCompletesContentInsideContentTag() {
        myFixture.addFileToProject("themes/bcc/content/blocks/intro.htm", "");
        myFixture.addFileToProject("themes/bcc/content/legal/terms.md", "");
        PsiFile page = myFixture.addFileToProject("themes/bcc/pages/journal/list.htm", """
            url = "/bcc-journal"
            ==
            {% content "<caret>" %}
            """);
        myFixture.configureFromExistingVirtualFile(page.getVirtualFile());

        myFixture.completeBasic();

        Set<String> lookupStrings = lookupStrings();
        Path currentPath = Path.of(page.getVirtualFile().getPath());
        assertTrue(
            "lookup=" + lookupStrings
                + ", file=" + currentPath
                + ", content=" + OctoberThemeContentResolver.listContentNames(currentPath),
            lookupStrings.contains("blocks/intro.htm")
        );
    }

    public void testCompletesContentInsideContentFunction() {
        myFixture.addFileToProject("themes/bcc/content/blocks/intro.htm", "");
        myFixture.addFileToProject("themes/bcc/content/legal/terms.md", "");
        PsiFile page = myFixture.addFileToProject("themes/bcc/pages/journal/list.htm", """
            url = "/bcc-journal"
            ==
            {{ content('<caret>') }}
            """);
        myFixture.configureFromExistingVirtualFile(page.getVirtualFile());

        myFixture.completeBasic();

        Set<String> lookupStrings = lookupStrings();
        Path currentPath = Path.of(page.getVirtualFile().getPath());
        assertTrue(
            "lookup=" + lookupStrings
                + ", file=" + currentPath
                + ", content=" + OctoberThemeContentResolver.listContentNames(currentPath),
            lookupStrings.contains("blocks/intro.htm")
        );
    }

    public void testCompletesPagesInsidePageFilter() {
        myFixture.addFileToProject("themes/bcc/pages/about/press-center-new/post.htm", "");
        myFixture.addFileToProject("themes/bcc/pages/journal/list.htm", "");
        PsiFile page = myFixture.addFileToProject("themes/bcc/pages/home.htm", """
            url = "/"
            ==
            {{ '<caret>'|page }}
            """);
        myFixture.configureFromExistingVirtualFile(page.getVirtualFile());

        myFixture.completeBasic();

        assertTrue(lookupStrings().contains("about/press-center-new/post"));
    }

    public void testCompletesPagesInsidePageUrlFunction() {
        myFixture.addFileToProject("themes/bcc/pages/about/press-center-new/post.htm", "");
        myFixture.addFileToProject("themes/bcc/pages/journal/list.htm", "");
        PsiFile page = myFixture.addFileToProject("themes/bcc/pages/home.htm", """
            url = "/"
            ==
            {{ pageUrl('<caret>') }}
            """);
        myFixture.configureFromExistingVirtualFile(page.getVirtualFile());

        myFixture.completeBasic();

        assertTrue(lookupStrings().contains("about/press-center-new/post"));
    }

    public void testCompletesPartialsInsidePartialFunction() {
        myFixture.addFileToProject("themes/bcc/partials/journal/list-category.htm", "");
        myFixture.addFileToProject("themes/bcc/partials/shared/card.htm", "");
        PsiFile page = myFixture.addFileToProject("themes/bcc/pages/home.htm", """
            url = "/"
            ==
            {{ partial('<caret>') }}
            """);
        myFixture.configureFromExistingVirtualFile(page.getVirtualFile());

        myFixture.completeBasic();

        assertTrue(lookupStrings().contains("journal/list-category"));
    }

    public void testCompletesPartialsInsideAjaxPartialFunction() {
        myFixture.addFileToProject("themes/bcc/partials/counter.htm", "");
        myFixture.addFileToProject("themes/bcc/partials/shared/card.htm", "");
        PsiFile page = myFixture.addFileToProject("themes/bcc/pages/home.htm", """
            url = "/"
            ==
            {{ ajaxPartial('<caret>') }}
            """);
        myFixture.configureFromExistingVirtualFile(page.getVirtualFile());

        myFixture.completeBasic();

        assertTrue(lookupStrings().contains("counter"));
    }

    public void testCompletesComponentAliasesInsideComponentBlock() {
        myFixture.addFileToProject("plugins/webinsane/bcc/components/PressCenterPostsComponent.php", "<?php\nclass PressCenterPostsComponent {}\n");
        myFixture.addFileToProject("plugins/webinsane/bcc/Plugin.php", """
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
        myFixture.addFileToProject("plugins/acme/blog/components/LatestPosts.php", "<?php\nclass LatestPosts {}\n");
        PsiFile page = myFixture.addFileToProject("themes/bcc/pages/home.htm", """
            url = "/"
            [<caret>]
            ==
            """);
        myFixture.configureFromExistingVirtualFile(page.getVirtualFile());

        myFixture.completeBasic();

        Set<String> lookupStrings = lookupStrings();
        assertTrue(lookupStrings.contains("PressCenterPosts"));
        assertTrue(lookupStrings.contains("LatestPosts"));
        assertEquals("Webinsane.Bcc", lookupTypeText("PressCenterPosts"));
        assertEquals("acme.blog", lookupTypeText("LatestPosts"));
        assertFalse(lookupStrings.contains("/plugins/vdlp/redirect/assets/images/icon.svg"));
        assertFalse(lookupStrings.contains("https://octobercms.com/plugin/vdlp-redirect"));
        assertFalse(lookupStrings.contains("vdlp.redirect::lang.permission.access_redirects.label"));
        assertFalse(lookupStrings.contains("vdlp.redirect::lang.permission.access_redirects.tab"));
    }

    public void testCompletesComponentPropertiesInsideComponentBlock() {
        myFixture.addFileToProject("plugins/webinsane/bcc/components/PressCenterPostsComponent.php", """
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
                        'category' => [
                            'title' => 'Category',
                            'type' => 'dropdown',
                        ],
                    ];
                }
            }
            """);
        myFixture.addFileToProject("plugins/webinsane/bcc/Plugin.php", """
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
        PsiFile page = myFixture.addFileToProject("themes/bcc/pages/home.htm", """
            url = "/"
            [PressCenterPosts]
            <caret>
            ==
            """);
        myFixture.configureFromExistingVirtualFile(page.getVirtualFile());

        myFixture.completeBasic();

        Set<String> lookupStrings = lookupStrings();
        assertTrue("lookup=" + lookupStrings, lookupStrings.contains("slug"));
        assertTrue("lookup=" + lookupStrings, lookupStrings.contains("category"));
        assertEquals("string", lookupTypeText("slug"));
        assertEquals(" Slug", lookupTailText("slug"));
        assertFalse(lookupStrings.contains("title"));
        assertFalse(lookupStrings.contains("type"));
        assertFalse(lookupStrings.contains("style"));
        assertFalse(lookupStrings.contains("<style"));
        assertFalse(lookupStrings.contains("script"));
        assertFalse(lookupStrings.contains("<script"));
    }

    public void testCompletesPropertiesForCurrentComponentBlockOnly() {
        myFixture.addFileToProject("plugins/webinsane/bcc/components/PressCenterPostsComponent.php", """
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
                    ];
                }
            }
            """);
        myFixture.addFileToProject("plugins/webinsane/bcc/components/AboutPages.php", """
            <?php

            namespace Webinsane\\Bcc\\Components;

            class AboutPages
            {
                public function defineProperties()
                {
                    return [
                        'headline' => [
                            'title' => 'Headline',
                            'type' => 'string',
                        ],
                        'heroTitle' => [
                            'title' => 'Hero title',
                            'type' => 'string',
                        ],
                    ];
                }
            }
            """);
        myFixture.addFileToProject("plugins/webinsane/bcc/Plugin.php", """
            <?php

            namespace Webinsane\\Bcc;

            use Webinsane\\Bcc\\Components\\AboutPages;
            use Webinsane\\Bcc\\Components\\PressCenterPostsComponent;

            class Plugin
            {
                public function registerComponents()
                {
                    return [
                        PressCenterPostsComponent::class => 'PressCenterPosts',
                        AboutPages::class => 'AboutPages',
                    ];
                }
            }
            """);
        PsiFile page = myFixture.addFileToProject("themes/bcc/pages/home.htm", """
            url = "/"
            [PressCenterPosts]
            slug = "{{ :slug }}"
            [AboutPages]
            h<caret>
            ==
            """);
        myFixture.configureFromExistingVirtualFile(page.getVirtualFile());

        myFixture.completeBasic();

        Set<String> lookupStrings = lookupStrings();
        assertTrue("lookup=" + lookupStrings, lookupStrings.contains("headline"));
        assertTrue("lookup=" + lookupStrings, lookupStrings.contains("heroTitle"));
        assertFalse(lookupStrings.contains("slug"));
        assertFalse(lookupStrings.contains("style"));
        assertFalse(lookupStrings.contains("<style"));
        assertFalse(lookupStrings.contains("script"));
        assertFalse(lookupStrings.contains("<script"));
    }

    public void testInsertsComponentPropertyDefaultValueAndMovesCaretIntoValue() {
        myFixture.addFileToProject("plugins/webinsane/bcc/components/PressCenterPostsComponent.php", """
            <?php

            namespace Webinsane\\Bcc\\Components;

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
                    ];
                }
            }
            """);
        myFixture.addFileToProject("plugins/webinsane/bcc/Plugin.php", """
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
        PsiFile page = myFixture.addFileToProject("themes/bcc/pages/home.htm", """
            url = "/"
            [PressCenterPosts]
            sl<caret>
            ==
            """);
        myFixture.configureFromExistingVirtualFile(page.getVirtualFile());

        myFixture.completeBasic();

        String text = myFixture.getEditor().getDocument().getText();
        assertTrue(text.contains("slug = \"{{ :slug }}\""));
        assertEquals(
            text.indexOf("{{ :slug }}") + "{{ :slug }}".length(),
            myFixture.getEditor().getCaretModel().getOffset()
        );
    }

    public void testTypingComponentAliasOpensCompletionPopupInsideComponentBlock() {
        myFixture.addFileToProject("plugins/webinsane/bcc/components/PressCenterPostsComponent.php", "<?php\nclass PressCenterPostsComponent {}\n");
        myFixture.addFileToProject("plugins/webinsane/bcc/components/PressPosts.php", "<?php\nclass PressPosts {}\n");
        myFixture.addFileToProject("plugins/webinsane/bcc/Plugin.php", """
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
        PsiFile page = myFixture.addFileToProject("themes/bcc/pages/home.htm", """
            url = "/"
            [<caret>]
            ==
            """);
        myFixture.configureFromExistingVirtualFile(page.getVirtualFile());

        myFixture.type('P');
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        Lookup lookup = LookupManager.getActiveLookup(myFixture.getEditor());
        assertNotNull(lookup);
        assertNotNull(lookup.getCurrentItem());
        assertEquals("PressCenterPosts", lookup.getCurrentItem().getLookupString());
    }

    private Set<String> lookupStrings() {
        LookupElement[] elements = myFixture.getLookupElements();
        assertNotNull(elements);
        Set<String> lookupStrings = Arrays.stream(elements)
            .map(LookupElement::getLookupString)
            .collect(Collectors.toSet());
        assertEquals(
            "duplicates in lookup: " + Arrays.toString(elements),
            elements.length,
            lookupStrings.size()
        );
        return lookupStrings;
    }

    private String lookupTypeText(String lookupString) {
        LookupElement[] elements = myFixture.getLookupElements();
        assertNotNull(elements);
        return Arrays.stream(elements)
            .filter(element -> lookupString.equals(element.getLookupString()))
            .findFirst()
            .map(LookupElementPresentation::renderElement)
            .map(LookupElementPresentation::getTypeText)
            .orElseThrow();
    }

    private String lookupTailText(String lookupString) {
        LookupElement[] elements = myFixture.getLookupElements();
        assertNotNull(elements);
        return Arrays.stream(elements)
            .filter(element -> lookupString.equals(element.getLookupString()))
            .findFirst()
            .map(LookupElementPresentation::renderElement)
            .map(LookupElementPresentation::getTailText)
            .orElseThrow();
    }
}
