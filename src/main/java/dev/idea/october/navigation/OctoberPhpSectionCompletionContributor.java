package dev.idea.october.navigation;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElementBuilder;

import java.util.Optional;

public final class OctoberPhpSectionCompletionContributor extends CompletionContributor {
    @Override
    public void beforeCompletion(CompletionInitializationContext context) {
        String documentText = context.getEditor().getDocument().getText();
        if (OctoberPhpSectionCompletionContext.find(documentText, context.getStartOffset()).isPresent()) {
            context.setDummyIdentifier("");
        }

        super.beforeCompletion(context);
    }

    @Override
    public void fillCompletionVariants(CompletionParameters parameters, CompletionResultSet result) {
        String documentText = parameters.getEditor().getDocument().getText();
        Optional<OctoberPhpSectionCompletionContext.Context> context =
            OctoberPhpSectionCompletionContext.find(documentText, parameters.getOffset());
        context.ifPresent(phpContext -> addPhpSectionCompletions(result, documentText, phpContext));

        super.fillCompletionVariants(parameters, result);
    }

    private static void addPhpSectionCompletions(
        CompletionResultSet result,
        String documentText,
        OctoberPhpSectionCompletionContext.Context completionContext
    ) {
        CompletionResultSet prefixedResult = result.withPrefixMatcher(completionContext.prefix()).caseInsensitive();
        for (String variable : OctoberPhpSectionCompletion.localVariables(
            documentText,
            completionContext.phpSection(),
            completionContext.caretOffset()
        )) {
            prefixedResult.addElement(
                LookupElementBuilder.create(variable)
                    .withLookupString(variable.substring(1))
                    .withTypeText("PHP variable", true)
            );
        }

        for (OctoberPhpSectionCompletion.Item item : OctoberPhpSectionCompletion.items()) {
            LookupElementBuilder element = LookupElementBuilder.create(item.lookupString())
                .withTypeText(item.typeText(), true);
            if (!item.tailText().isEmpty() || item.caretShift() != 0) {
                element = element.withInsertHandler((context, lookupItem) -> insertPhpSectionTail(context, item));
            }

            prefixedResult.addElement(element);
        }
    }

    private static void insertPhpSectionTail(InsertionContext context, OctoberPhpSectionCompletion.Item item) {
        int tailOffset = context.getTailOffset();
        if (!item.tailText().isEmpty()) {
            context.getDocument().insertString(tailOffset, item.tailText());
        }
        context.getEditor().getCaretModel().moveToOffset(tailOffset + item.caretShift());
    }
}
