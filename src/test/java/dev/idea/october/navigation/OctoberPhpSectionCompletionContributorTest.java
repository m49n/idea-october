package dev.idea.october.navigation;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class OctoberPhpSectionCompletionContributorTest extends BasePlatformTestCase {
    public void testCompletesLifecycleMethodsInsidePhpSection() {
        PsiFile file = myFixture.addFileToProject("themes/bcc/pages/journal/list.htm", """
            url = "/bcc-journal"
            ==
            function onS<caret>
            ==
            {% partial "journal/list-category" %}
            """);
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

        myFixture.completeBasic();

        PsiElement elementAtCaret = file.findElementAt(myFixture.getCaretOffset() - 1);
        assertNotNull(elementAtCaret);
        assertEquals("XML", elementAtCaret.getLanguage().getID());
        assertTrue(lookupStrings().contains("onStart"));
    }

    public void testCompletesThisHelpersInsidePhpSection() {
        PsiFile file = myFixture.addFileToProject("themes/bcc/pages/journal/list.htm", """
            url = "/bcc-journal"
            ==
            $thi<caret>
            ==
            {% partial "journal/list-category" %}
            """);
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

        myFixture.completeBasic();

        Set<String> lookupStrings = lookupStrings();
        assertTrue(lookupStrings.contains("$this['']"));
        assertTrue(lookupStrings.contains("$this->param('')"));
    }

    public void testCompletesLocalVariablesInsidePhpSectionWithoutTypedDollar() {
        PsiFile file = myFixture.addFileToProject("themes/bcc/pages/journal/list.htm", """
            url = "/bcc-journal"
            ==
            function onStart()
            {
                $qq = 'qwe';
                $this['category'] = $this->param('category');
                $this['qq'] = q<caret>
            }
            ==
            {% partial "journal/list-category" %}
            """);
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

        myFixture.completeBasic();

        myFixture.checkResult("""
            url = "/bcc-journal"
            ==
            function onStart()
            {
                $qq = 'qwe';
                $this['category'] = $this->param('category');
                $this['qq'] = $qq<caret>
            }
            ==
            {% partial "journal/list-category" %}
            """);
    }

    public void testTypingLetterOpensLocalVariableCompletionInsidePhpSection() {
        PsiFile file = myFixture.addFileToProject("themes/bcc/pages/journal/list.htm", """
            url = "/bcc-journal"
            ==
            function onStart()
            {
                $qq = 'qwe';
                $this['qq'] = <caret>
            }
            ==
            {% partial "journal/list-category" %}
            """);
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

        myFixture.type('q');
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        Set<String> lookupStrings = lookupStrings();
        assertTrue(lookupStrings.contains("$qq"));
        Lookup lookup = LookupManager.getActiveLookup(myFixture.getEditor());
        assertNotNull(lookup);
        LookupElement currentItem = lookup.getCurrentItem();
        assertNotNull(currentItem);
        assertEquals("$qq", currentItem.getLookupString());
    }

    public void testEnterAcceptsSelectedAutoPopupItemInsidePhpSection() {
        PsiFile file = myFixture.addFileToProject("themes/bcc/pages/journal/list.htm", """
            url = "/bcc-journal"
            ==
            function onStart()
            {
                $qq = 'qwe';
                $this['qq'] = <caret>
            }
            ==
            {% partial "journal/list-category" %}
            """);
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

        myFixture.type('q');
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ENTER);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        myFixture.checkResult("""
            url = "/bcc-journal"
            ==
            function onStart()
            {
                $qq = 'qwe';
                $this['qq'] = $qq<caret>
            }
            ==
            {% partial "journal/list-category" %}
            """);
    }

    private Set<String> lookupStrings() {
        LookupElement[] elements = myFixture.getLookupElements();
        assertNotNull(elements);
        return Arrays.stream(elements)
            .map(LookupElement::getLookupString)
            .collect(Collectors.toSet());
    }
}
