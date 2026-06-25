package dev.idea.october.navigation;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class OctoberInspectionFileTest extends BasePlatformTestCase {
    public void testAcceptsBaseThemeTemplateFile() {
        PsiFile page = myFixture.addFileToProject("themes/bcc/pages/home.htm", """
            url = "/"
            ==
            """);

        assertTrue(OctoberInspectionFile.shouldInspectThemeTemplate(page));
    }

    public void testRejectsFilesOutsideThemeTemplates() {
        PsiFile readme = myFixture.addFileToProject("README.md", "# Test");

        assertFalse(OctoberInspectionFile.shouldInspectThemeTemplate(readme));
    }
}
