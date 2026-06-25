package dev.idea.october.navigation;

import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class OctoberPhpSectionTypingTest extends BasePlatformTestCase {
    public void testEnterIndentsBlankLineInsidePhpSection() {
        PsiFile file = myFixture.addFileToProject("themes/bcc/pages/journal/list.htm", """
            url = "/bcc-journal"
            ==
            function onStart()
            {<caret>
            }
            ==
            {% partial "journal/list-category" %}
            """);
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ENTER);

        myFixture.checkResult("""
            url = "/bcc-journal"
            ==
            function onStart()
            {
                <caret>
            }
            ==
            {% partial "journal/list-category" %}
            """);
    }

    public void testClosingBraceIsReindentedInsidePhpSection() {
        PsiFile file = myFixture.addFileToProject("themes/bcc/pages/journal/list.htm", """
            url = "/bcc-journal"
            ==
            function onStart()
            {
                if ($this->param('category')) {
                    $this['category'] = true;
                    <caret>
                }
            }
            ==
            {% partial "journal/list-category" %}
            """);
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

        myFixture.type('}');

        myFixture.checkResult("""
            url = "/bcc-journal"
            ==
            function onStart()
            {
                if ($this->param('category')) {
                    $this['category'] = true;
                }<caret>
                }
            }
            ==
            {% partial "journal/list-category" %}
            """);
    }
}
