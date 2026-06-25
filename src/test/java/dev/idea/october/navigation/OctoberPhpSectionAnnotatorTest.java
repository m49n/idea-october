package dev.idea.october.navigation;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.List;

public class OctoberPhpSectionAnnotatorTest extends BasePlatformTestCase {
    public void testHighlightsPhpSyntaxInThemePageCodeSection() {
        PsiFile file = myFixture.addFileToProject("themes/bcc/pages/journal/list.htm", """
            url = "/bcc-journal"
            layout = "main"
            title = "Press"

            [PressCenterPosts]
            slug = "{{:slug}}"
            ==
            function onStart()
            {
                $this['category'] = $this->param('category');
            }
            ==
            {% partial "journal/list-category" category=category list=categories %}
            """);
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

        TextRange phpRange = OctoberTemplateSectionParser.findPhpSection(file.getText()).orElseThrow();
        List<HighlightInfo> highlights = myFixture.doHighlighting(HighlightSeverity.TEXT_ATTRIBUTES);

        boolean hasPhpKeywordHighlight = highlights.stream().anyMatch(highlight ->
            highlight.startOffset >= phpRange.getStartOffset()
                && highlight.endOffset <= phpRange.getEndOffset()
                && "function".equals(file.getText().substring(highlight.startOffset, highlight.endOffset))
                && highlight.forcedTextAttributesKey != null
        );

        assertTrue(highlightDiagnostics(file, highlights), hasPhpKeywordHighlight);
    }

    private static String highlightDiagnostics(PsiFile file, List<HighlightInfo> highlights) {
        TextRange phpRange = OctoberTemplateSectionParser.findPhpSection(file.getText()).orElse(TextRange.EMPTY_RANGE);
        StringBuilder builder = new StringBuilder("No PHP keyword highlight found. PHP range: ")
            .append(phpRange)
            .append(System.lineSeparator())
            .append("File language: ")
            .append(file.getLanguage().getID())
            .append(System.lineSeparator())
            .append("Highlights:")
            .append(System.lineSeparator());

        for (HighlightInfo highlight : highlights) {
            if (TextRange.create(highlight.startOffset, highlight.endOffset).intersects(phpRange)) {
                builder
                    .append(highlight.startOffset)
                    .append("-")
                    .append(highlight.endOffset)
                    .append(" key=")
                    .append(highlight.forcedTextAttributesKey)
                    .append(" text=")
                    .append(snippet(file.getText().substring(highlight.startOffset, highlight.endOffset)))
                    .append(System.lineSeparator());
            }
        }

        builder
            .append("Elements intersecting PHP range:")
            .append(System.lineSeparator());
        file.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                TextRange range = element.getTextRange();
                if (range != null && range.intersects(phpRange) && element.getFirstChild() == null) {
                    builder
                        .append(element.getClass().getName())
                        .append(" elementType=")
                        .append(element.getNode() == null ? "<none>" : element.getNode().getElementType())
                        .append(" language=")
                        .append(element.getLanguage().getID())
                        .append(" range=")
                        .append(range)
                        .append(" text=")
                        .append(snippet(element.getText()))
                        .append(System.lineSeparator());
                }
                super.visitElement(element);
            }
        });

        return builder.toString();
    }

    private static String snippet(String text) {
        return text.replace("\r", "\\r")
            .replace("\n", "\\n")
            .substring(0, Math.min(120, text.length()));
    }
}
