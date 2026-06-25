package dev.idea.october.navigation;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class OctoberMissingThemeFileInspectionTest extends BasePlatformTestCase {
    public void testHighlightsMissingThemeFiles() {
        myFixture.addFileToProject("themes/bcc/layouts/main.htm", "");
        myFixture.addFileToProject("themes/bcc/partials/journal/list-category.htm", "");
        myFixture.addFileToProject("themes/bcc/content/blocks/intro.htm", "");
        myFixture.addFileToProject("themes/bcc/pages/about/press-center-new/post.htm", "");
        myFixture.enableInspections(new OctoberMissingThemeFileInspection());

        PsiFile page = myFixture.addFileToProject("themes/bcc/pages/journal/list.htm", """
            url = "/bcc-journal"
            layout = "main"
            ==
            {% partial "journal/list-category" %}
            {% partial "<warning descr="October partial 'journal/missing' not found">journal/missing</warning>" %}
            {% content "blocks/intro" %}
            {% content "<warning descr="October content 'blocks/missing' not found">blocks/missing</warning>" %}
            {{ 'about/press-center-new/post'|page }}
            {{ '<warning descr="October page 'missing/page' not found">missing/page</warning>'|page }}
            """);
        myFixture.configureFromExistingVirtualFile(page.getVirtualFile());

        myFixture.checkHighlighting();
    }

    public void testCreateMissingPartialQuickFixCreatesThemeFile() {
        assertQuickFixCreatesThemeFile(
            """
                url = "/bcc-journal"
                ==
                {% partial "journal/mis<caret>sing" %}
                """,
            "Create October partial 'journal/missing'",
            "themes/bcc/partials/journal/missing.htm"
        );
    }

    public void testCreateMissingLayoutQuickFixCreatesThemeFile() {
        assertQuickFixCreatesThemeFile(
            """
                url = "/bcc-journal"
                layout = "ma<caret>in"
                ==
                """,
            "Create October layout 'main'",
            "themes/bcc/layouts/main.htm"
        );
    }

    public void testCreateMissingContentQuickFixCreatesThemeFile() {
        assertQuickFixCreatesThemeFile(
            """
                url = "/bcc-journal"
                ==
                {% content "blocks/mis<caret>sing" %}
                """,
            "Create October content 'blocks/missing'",
            "themes/bcc/content/blocks/missing.htm"
        );
    }

    public void testCreateMissingPageQuickFixCreatesThemeFile() {
        assertQuickFixCreatesThemeFile(
            """
                url = "/bcc-journal"
                ==
                {{ 'missing/pa<caret>ge'|page }}
                """,
            "Create October page 'missing/page'",
            "themes/bcc/pages/missing/page.htm"
        );
    }

    public void testCreateMissingPageUrlQuickFixCreatesThemeFile() {
        assertQuickFixCreatesThemeFile(
            """
                url = "/bcc-journal"
                ==
                {{ pageUrl('missing/pa<caret>ge') }}
                """,
            "Create October page 'missing/page'",
            "themes/bcc/pages/missing/page.htm"
        );
    }

    public void testCreateMissingPartialFunctionQuickFixCreatesThemeFile() {
        assertQuickFixCreatesThemeFile(
            """
                url = "/bcc-journal"
                ==
                {{ partial('journal/mis<caret>sing') }}
                """,
            "Create October partial 'journal/missing'",
            "themes/bcc/partials/journal/missing.htm"
        );
    }

    private void assertQuickFixCreatesThemeFile(
        String pageText,
        String intentionName,
        String expectedPath
    ) {
        myFixture.enableInspections(new OctoberMissingThemeFileInspection());
        PsiFile page = myFixture.addFileToProject("themes/bcc/pages/journal/list.htm", pageText);
        myFixture.configureFromExistingVirtualFile(page.getVirtualFile());
        myFixture.doHighlighting();

        myFixture.launchAction(myFixture.findSingleIntention(intentionName));

        assertNotNull(myFixture.findFileInTempDir(expectedPath));
    }
}
