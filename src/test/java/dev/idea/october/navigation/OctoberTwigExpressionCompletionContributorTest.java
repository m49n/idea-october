package dev.idea.october.navigation;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class OctoberTwigExpressionCompletionContributorTest extends BasePlatformTestCase {
    public void testCompletesVariablesDeclaredAboveCaret() {
        configurePage("""
            {% set someVar = 123 %}
            {% set someOther = 456 %}
            {{ some<caret> }}
            """);

        myFixture.completeBasic();

        Set<String> lookupStrings = lookupStrings();
        assertTrue(lookupStrings.contains("someVar"));
        assertTrue(lookupStrings.contains("someOther"));
    }

    public void testCompletesVariablesFromActiveNestedLoops() {
        configurePage("""
            {% for album in albums %}
                {% for key, photo in album.photos %}
                    {{ <caret> }}
                {% endfor %}
            {% endfor %}
            """);

        myFixture.completeBasic();

        Set<String> lookupStrings = lookupStrings();
        assertTrue(lookupStrings.contains("album"));
        assertTrue(lookupStrings.contains("key"));
        assertTrue(lookupStrings.contains("photo"));
    }

    public void testDoesNotCompleteVariableBeforeSetDeclaration() {
        configurePage("""
            {{ <caret> }}
            {% set laterVariable = 123 %}
            """);

        myFixture.completeBasic();

        assertFalse(lookupStrings().contains("laterVariable"));
    }

    public void testDoesNotCompleteLoopVariableAfterEndFor() {
        configurePage("""
            {% for photo in photos %}
                {{ photo }}
            {% endfor %}
            {{ <caret> }}
            """);

        myFixture.completeBasic();

        assertFalse(lookupStrings().contains("photo"));
    }

    public void testCompletesOctoberFiltersWithoutDuplicates() {
        configurePage("{{ value | <caret> }}");

        myFixture.completeBasic();

        assertTrue(lookupStrings().containsAll(OctoberTwigCompletionCatalog.filters()));
    }

    public void testCompletesOctoberThisPropertiesWithoutDuplicates() {
        configurePage("{{ this.<caret> }}");

        myFixture.completeBasic();

        assertTrue(lookupStrings().containsAll(OctoberTwigCompletionCatalog.thisProperties()));
    }

    public void testCompletesThisRootVariable() {
        configurePage("""
            {% set thing = 123 %}
            {{ thi<caret> }}
            """);

        myFixture.completeBasic();

        assertTrue(lookupStrings().contains("this"));
    }

    public void testTypingVariableOpensFocusedCompletionPopup() {
        configurePage("""
            {% set someVar = 123 %}
            {% set secondVar = 456 %}
            {{ <caret> }}
            """);

        myFixture.type('s');
        assertFocusedLookup();
    }

    public void testTypingPipeOpensFocusedFilterCompletionPopup() {
        configurePage("{{ value <caret> }}");

        myFixture.type('|');
        assertFocusedLookup();
        assertTrue(activeLookupStrings().contains("page"));
    }

    public void testTypingDotAfterThisOpensFocusedPropertyCompletionPopup() {
        configurePage("{{ this<caret> }}");

        myFixture.type('.');
        assertFocusedLookup();
        assertTrue(activeLookupStrings().contains("page"));
    }

    public void testTypingOctoberTagNameOpensFocusedCompletionPopup() {
        configurePage("{% <caret> %}");

        myFixture.type('p');
        assertFocusedLookup();
        assertTrue(activeLookupStrings().contains("partial"));
    }

    public void testTypingNumberDoesNotOpenOctoberCompletionPopup() {
        configurePage("{{ <caret> }}");

        myFixture.type('1');
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertNull(LookupManager.getActiveLookup(myFixture.getEditor()));
    }

    private void configurePage(String markup) {
        PsiFile page = myFixture.addFileToProject("themes/bcc/pages/home.htm", """
            url = "/"
            ==
            """ + markup);
        myFixture.configureFromExistingVirtualFile(page.getVirtualFile());
    }

    private void assertFocusedLookup() {
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        Lookup lookup = LookupManager.getActiveLookup(myFixture.getEditor());
        assertNotNull(lookup);
        assertNotNull(lookup.getCurrentItem());
    }

    private Set<String> activeLookupStrings() {
        Lookup lookup = LookupManager.getActiveLookup(myFixture.getEditor());
        assertNotNull(lookup);
        return lookup.getItems().stream()
            .map(LookupElement::getLookupString)
            .collect(Collectors.toSet());
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
}
