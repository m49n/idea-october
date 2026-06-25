package dev.idea.october.navigation;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class OctoberMissingComponentInspectionTest extends BasePlatformTestCase {
    public void testHighlightsMissingComponentAlias() {
        myFixture.addFileToProject("plugins/webinsane/bcc/components/PressCenterPostsComponent.php", "<?php\nclass PressCenterPostsComponent {}\n");
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
        myFixture.enableInspections(new OctoberMissingComponentInspection());

        PsiFile page = myFixture.addFileToProject("themes/bcc/pages/home.htm", """
            url = "/"
            [PressCenterPosts]
            [<warning descr="October component 'MissingComponent' not found">MissingComponent</warning>]
            ==
            """);
        myFixture.configureFromExistingVirtualFile(page.getVirtualFile());

        myFixture.checkHighlighting();
    }
}
