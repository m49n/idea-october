package dev.idea.october.navigation;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class OctoberPhpSectionPostFormatProcessorTest extends BasePlatformTestCase {
    public void testFormatsPhpSectionInThemePage() {
        PsiFile file = myFixture.addFileToProject("themes/bcc/pages/journal/list.htm", """
            url = "/bcc-journal"
            ==
            function onStart()
            {
            $this['category'] = $this->param('category');
            }
            ==
            {% partial "journal/list-category" %}
            """);
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

        WriteCommandAction.runWriteCommandAction(getProject(), (Runnable) () ->
            CodeStyleManager.getInstance(getProject()).reformatText(file, 0, file.getTextLength())
        );

        assertTrue(file.getText().contains("""
            function onStart()
            {
                $this['category'] = $this->param('category');
            }
            """.stripTrailing()));
    }
}
