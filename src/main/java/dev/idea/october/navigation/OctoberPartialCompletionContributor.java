package dev.idea.october.navigation;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class OctoberPartialCompletionContributor extends CompletionContributor {
    @Override
    public void beforeCompletion(CompletionInitializationContext context) {
        String documentText = context.getEditor().getDocument().getText();
        if (
            OctoberPartialCompletionContext.find(documentText, context.getStartOffset()).isPresent()
                || OctoberLayoutCompletionContext.find(documentText, context.getStartOffset()).isPresent()
                || OctoberContentCompletionContext.find(documentText, context.getStartOffset()).isPresent()
                || OctoberPageCompletionContext.find(documentText, context.getStartOffset()).isPresent()
                || OctoberComponentCompletionContext.find(documentText, context.getStartOffset()).isPresent()
                || OctoberComponentPropertyCompletionContext.find(documentText, context.getStartOffset()).isPresent()
                || OctoberTwigTagCompletionContext.find(documentText, context.getStartOffset()).isPresent()
        ) {
            if (!context.getDummyIdentifier().isEmpty()) {
                context.setDummyIdentifier("");
            }
        }

        super.beforeCompletion(context);
    }

    @Override
    public void fillCompletionVariants(CompletionParameters parameters, CompletionResultSet result) {
        String documentText = parameters.getEditor().getDocument().getText();
        java.util.Optional<OctoberPartialCompletionContext.Context> partialContext =
            OctoberPartialCompletionContext.find(documentText, parameters.getOffset());
        if (partialContext.isPresent()) {
            addPartialCompletions(parameters, result, partialContext.get().prefix());
            super.fillCompletionVariants(parameters, result);
            return;
        }

        java.util.Optional<OctoberLayoutCompletionContext.Context> layoutContext =
            OctoberLayoutCompletionContext.find(documentText, parameters.getOffset());
        if (layoutContext.isPresent()) {
            addLayoutCompletions(parameters, result, layoutContext.get().prefix());
            super.fillCompletionVariants(parameters, result);
            return;
        }

        java.util.Optional<OctoberContentCompletionContext.Context> contentContext =
            OctoberContentCompletionContext.find(documentText, parameters.getOffset());
        if (contentContext.isPresent()) {
            addContentCompletions(parameters, result, contentContext.get().prefix());
            super.fillCompletionVariants(parameters, result);
            return;
        }

        java.util.Optional<OctoberPageCompletionContext.Context> pageContext =
            OctoberPageCompletionContext.find(documentText, parameters.getOffset());
        if (pageContext.isPresent()) {
            addPageCompletions(parameters, result, pageContext.get().prefix());
            super.fillCompletionVariants(parameters, result);
            return;
        }

        java.util.Optional<OctoberComponentCompletionContext.Context> componentContext =
            OctoberComponentCompletionContext.find(documentText, parameters.getOffset());
        if (componentContext.isPresent()) {
            addComponentCompletions(parameters, result, componentContext.get().prefix());
            super.fillCompletionVariants(parameters, result);
            return;
        }

        java.util.Optional<OctoberComponentPropertyCompletionContext.Context> componentPropertyContext =
            OctoberComponentPropertyCompletionContext.find(documentText, parameters.getOffset());
        if (componentPropertyContext.isPresent()) {
            addComponentPropertyCompletions(parameters, result, componentPropertyContext.get());
            result.stopHere();
            return;
        }

        OctoberTwigTagCompletionContext.find(documentText, parameters.getOffset())
            .ifPresent(context -> addTwigTagCompletions(result, context.prefix()));

        super.fillCompletionVariants(parameters, result);
    }

    private static void addPartialCompletions(
        CompletionParameters parameters,
        CompletionResultSet result,
        String prefix
    ) {
        VirtualFile currentFile = findCurrentVirtualFile(parameters.getPosition());
        if (currentFile == null) {
            return;
        }

        CompletionResultSet prefixedResult = result.withPrefixMatcher(prefix).caseInsensitive();
        for (String partialName : listThemeFileNames(currentFile, OctoberThemeFileType.PARTIAL)) {
            prefixedResult.addElement(
                LookupElementBuilder.create(partialName)
                    .withTypeText("October partial", true)
            );
        }
    }

    private static void addComponentCompletions(
        CompletionParameters parameters,
        CompletionResultSet result,
        String prefix
    ) {
        VirtualFile currentFile = findCurrentVirtualFile(parameters.getPosition());
        if (currentFile == null) {
            return;
        }

        CompletionResultSet prefixedResult = result.withPrefixMatcher(prefix).caseInsensitive();
        for (OctoberComponentAlias componentAlias : OctoberComponentAliasProvider.listComponentAliasMetadata(currentFile)) {
            prefixedResult.addElement(
                LookupElementBuilder.create(componentAlias.alias())
                    .withTypeText(componentAlias.owner(), true)
            );
        }
    }

    private static void addComponentPropertyCompletions(
        CompletionParameters parameters,
        CompletionResultSet result,
        OctoberComponentPropertyCompletionContext.Context context
    ) {
        VirtualFile currentFile = findCurrentVirtualFile(parameters.getPosition());
        if (currentFile == null) {
            return;
        }

        CompletionResultSet prefixedResult = result.withPrefixMatcher(context.prefix()).caseInsensitive();
        for (OctoberComponentProperty property : OctoberComponentPropertyProvider.listProperties(
            currentFile,
            context.componentAlias()
        )) {
            prefixedResult.addElement(
                LookupElementBuilder.create(property.name())
                    .withTypeText("October component property", true)
                    .withInsertHandler((insertionContext, item) -> insertComponentPropertyAssignment(
                        insertionContext,
                        property
                    ))
            );
        }
    }

    private static void insertComponentPropertyAssignment(
        InsertionContext context,
        OctoberComponentProperty property
    ) {
        int tailOffset = context.getTailOffset();
        String defaultValue = property.defaultValue();
        String assignmentText;
        int caretOffset;
        if (defaultValue == null) {
            assignmentText = " = \"\"";
            caretOffset = tailOffset + " = \"".length();
        }
        else if (property.quotedDefaultValue()) {
            String escapedDefaultValue = escapeQuotedConfigValue(defaultValue);
            assignmentText = " = \"" + escapedDefaultValue + "\"";
            caretOffset = tailOffset + " = \"".length() + escapedDefaultValue.length();
        }
        else {
            assignmentText = " = " + defaultValue;
            caretOffset = tailOffset + " = ".length() + defaultValue.length();
        }

        context.getDocument().insertString(tailOffset, assignmentText);
        context.setTailOffset(tailOffset + assignmentText.length());
        context.getEditor().getCaretModel().moveToOffset(caretOffset);
    }

    private static String escapeQuotedConfigValue(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void addPageCompletions(
        CompletionParameters parameters,
        CompletionResultSet result,
        String prefix
    ) {
        VirtualFile currentFile = findCurrentVirtualFile(parameters.getPosition());
        if (currentFile == null) {
            return;
        }

        CompletionResultSet prefixedResult = result.withPrefixMatcher(prefix).caseInsensitive();
        for (String pageName : listThemeFileNames(currentFile, OctoberThemeFileType.PAGE)) {
            prefixedResult.addElement(
                LookupElementBuilder.create(pageName)
                    .withTypeText("October page", true)
            );
        }
    }

    private static void addLayoutCompletions(
        CompletionParameters parameters,
        CompletionResultSet result,
        String prefix
    ) {
        VirtualFile currentFile = findCurrentVirtualFile(parameters.getPosition());
        if (currentFile == null) {
            return;
        }

        CompletionResultSet prefixedResult = result.withPrefixMatcher(prefix).caseInsensitive();
        for (String layoutName : listThemeFileNames(currentFile, OctoberThemeFileType.LAYOUT)) {
            prefixedResult.addElement(
                LookupElementBuilder.create(layoutName)
                    .withTypeText("October layout", true)
            );
        }
    }

    private static void addContentCompletions(
        CompletionParameters parameters,
        CompletionResultSet result,
        String prefix
    ) {
        VirtualFile currentFile = findCurrentVirtualFile(parameters.getPosition());
        if (currentFile == null) {
            return;
        }

        CompletionResultSet prefixedResult = result.withPrefixMatcher(prefix).caseInsensitive();
        for (String contentName : listThemeFileNames(currentFile, OctoberThemeFileType.CONTENT)) {
            prefixedResult.addElement(
                LookupElementBuilder.create(contentName)
                    .withTypeText("October content", true)
            );
        }
    }

    private static List<String> listThemeFileNames(VirtualFile currentFile, OctoberThemeFileType type) {
        List<String> names = listThemeFileNames(Path.of(currentFile.getPath()), type);
        if (!names.isEmpty()) {
            return names;
        }

        return listThemeFileNamesFromVirtualFile(currentFile, type);
    }

    private static List<String> listThemeFileNames(Path currentPath, OctoberThemeFileType type) {
        return switch (type) {
            case PARTIAL -> OctoberThemePartialResolver.listPartialNames(currentPath);
            case LAYOUT -> OctoberThemeLayoutResolver.listLayoutNames(currentPath);
            case CONTENT -> OctoberThemeContentResolver.listContentNames(currentPath);
            case PAGE -> OctoberThemePageResolver.listPageNames(currentPath);
        };
    }

    private static List<String> listThemeFileNamesFromVirtualFile(VirtualFile currentFile, OctoberThemeFileType type) {
        VirtualFile themeRoot = findThemeRoot(currentFile);
        if (themeRoot == null) {
            return List.of();
        }

        VirtualFile typeRoot = themeRoot.findChild(type.directoryName());
        if (typeRoot == null || !typeRoot.isDirectory()) {
            return List.of();
        }

        List<String> names = new ArrayList<>();
        collectThemeFileNames(typeRoot, typeRoot, type, names);
        names.sort(Comparator.naturalOrder());
        return names;
    }

    private static void collectThemeFileNames(
        VirtualFile root,
        VirtualFile current,
        OctoberThemeFileType type,
        List<String> names
    ) {
        for (VirtualFile child : current.getChildren()) {
            if (child.isDirectory()) {
                collectThemeFileNames(root, child, type, names);
                continue;
            }

            if (!acceptsVirtualFile(type, child)) {
                continue;
            }

            String relativePath = child.getPath().substring(root.getPath().length() + 1).replace('\\', '/');
            names.add(type.indexedName(Path.of(relativePath)));
        }
    }

    private static boolean acceptsVirtualFile(OctoberThemeFileType type, VirtualFile file) {
        return type == OctoberThemeFileType.CONTENT || file.getName().endsWith(".htm");
    }

    private static VirtualFile findThemeRoot(VirtualFile currentFile) {
        VirtualFile current = currentFile.isDirectory() ? currentFile : currentFile.getParent();
        while (current != null) {
            VirtualFile parent = current.getParent();
            if (parent != null && "themes".equalsIgnoreCase(parent.getName())) {
                return current;
            }

            current = parent;
        }

        return null;
    }

    private static void addTwigTagCompletions(CompletionResultSet result, String prefix) {
        CompletionResultSet prefixedResult = result.withPrefixMatcher(prefix).caseInsensitive();
        for (OctoberTwigTagCompletion.Tag tag : OctoberTwigTagCompletion.tags()) {
            LookupElementBuilder element = LookupElementBuilder.create(tag.name())
                .withTypeText(tag.typeText(), true);
            if (!tag.tailText().isEmpty()) {
                element = element.withInsertHandler((context, item) -> insertTagTail(context, tag));
            }

            prefixedResult.addElement(element);
        }
    }

    private static void insertTagTail(InsertionContext context, OctoberTwigTagCompletion.Tag tag) {
        int tailOffset = context.getTailOffset();
        context.getDocument().insertString(tailOffset, tag.tailText());
        context.getEditor().getCaretModel().moveToOffset(tailOffset + tag.caretShift());
    }

    private static VirtualFile findCurrentVirtualFile(PsiElement position) {
        PsiFile topLevelFile = InjectedLanguageManager.getInstance(position.getProject()).getTopLevelFile(position);
        if (topLevelFile != null && topLevelFile.getVirtualFile() != null) {
            return topLevelFile.getVirtualFile();
        }

        PsiFile containingFile = position.getContainingFile();
        return containingFile == null ? null : containingFile.getVirtualFile();
    }
}
