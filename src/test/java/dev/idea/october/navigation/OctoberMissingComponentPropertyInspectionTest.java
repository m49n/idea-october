package dev.idea.october.navigation;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class OctoberMissingComponentPropertyInspectionTest extends BasePlatformTestCase {
    public void testHighlightsUnknownComponentProperty() {
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
        myFixture.enableInspections(new OctoberMissingComponentPropertyInspection());

        PsiFile page = myFixture.addFileToProject("themes/bcc/pages/home.htm", """
            url = "/"
            [PressCenterPosts]
            slug = "{{ :slug }}"
            <warning descr="October component property 'missing' not found on 'PressCenterPosts'. Available: slug">missing</warning> = "value"
            ==
            """);
        myFixture.configureFromExistingVirtualFile(page.getVirtualFile());

        myFixture.checkHighlighting();
    }

    public void testReportsUnknownComponentPropertyOnlyOnce() {
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
        myFixture.enableInspections(new OctoberMissingComponentPropertyInspection());

        PsiFile page = myFixture.addFileToProject("themes/bcc/pages/home.htm", """
            url = "/"
            [PressCenterPosts]
            slugg = "{{ :slug }}"
            ==
            function onStart()
            {
            }
            ==
            """);
        myFixture.configureFromExistingVirtualFile(page.getVirtualFile());

        long count = myFixture.doHighlighting().stream()
            .map(HighlightInfo::getDescription)
            .filter("October component property 'slugg' not found on 'PressCenterPosts'. Available: slug"::equals)
            .count();

        assertEquals(1, count);
    }
}
