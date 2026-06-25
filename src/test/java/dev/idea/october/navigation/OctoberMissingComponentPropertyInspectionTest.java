package dev.idea.october.navigation;

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
            <warning descr="October component property 'missing' not found on 'PressCenterPosts'">missing</warning> = "value"
            ==
            """);
        myFixture.configureFromExistingVirtualFile(page.getVirtualFile());

        myFixture.checkHighlighting();
    }
}
