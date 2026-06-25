package dev.idea.october.navigation;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
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

    public void testReportsMissingComponentAliasOnlyOnce() {
        myFixture.enableInspections(new OctoberMissingComponentInspection());

        PsiFile page = myFixture.addFileToProject("themes/bcc/pages/home.htm", """
            url = "/"
            [PressCenterPostss]
            ==
            function onStart()
            {
            }
            ==
            """);
        myFixture.configureFromExistingVirtualFile(page.getVirtualFile());

        long count = myFixture.doHighlighting().stream()
            .map(HighlightInfo::getDescription)
            .filter("October component 'PressCenterPostss' not found"::equals)
            .count();

        assertEquals(1, count);
    }
}
