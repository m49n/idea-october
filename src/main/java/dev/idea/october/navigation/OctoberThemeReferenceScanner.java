package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OctoberThemeReferenceScanner {
    private static final Pattern SECTION_SEPARATOR = Pattern.compile("(?m)^\\s*==\\s*$");
    private static final Pattern LAYOUT_SETTING = Pattern.compile("(?m)^\\s*layout\\s*=\\s*(['\"])([^'\"]+)\\1");
    private static final Pattern PARTIAL_TAG = Pattern.compile("\\{%\\s*(?:partial|ajaxPartial)\\s+(['\"])([^'\"]+)\\1");
    private static final Pattern PARTIAL_FUNCTION = Pattern.compile("\\b(?:partial|ajaxPartial)\\s*\\(\\s*(['\"])([^'\"]+)\\1");
    private static final Pattern CONTENT_TAG = Pattern.compile("\\{%\\s*content\\s+(['\"])([^'\"]+)\\1");
    private static final Pattern CONTENT_FUNCTION = Pattern.compile("\\bcontent\\s*\\(\\s*(['\"])([^'\"]+)\\1");
    private static final Pattern PAGE_FILTER = Pattern.compile("(['\"])([^'\"]+)\\1\\s*\\|\\s*page\\b");
    private static final Pattern PAGE_URL_FUNCTION = Pattern.compile("\\bpageUrl\\s*\\(\\s*(['\"])([^'\"]+)\\1");

    private OctoberThemeReferenceScanner() {
    }

    public static @NotNull List<Reference> scan(@NotNull String fileText) {
        List<Reference> references = new ArrayList<>();

        scanLayoutReferences(fileText, references);
        scanReferences(fileText, references, PARTIAL_TAG, OctoberThemeFileType.PARTIAL);
        scanReferences(fileText, references, PARTIAL_FUNCTION, OctoberThemeFileType.PARTIAL);
        scanReferences(fileText, references, CONTENT_TAG, OctoberThemeFileType.CONTENT);
        scanReferences(fileText, references, CONTENT_FUNCTION, OctoberThemeFileType.CONTENT);
        scanReferences(fileText, references, PAGE_FILTER, OctoberThemeFileType.PAGE);
        scanReferences(fileText, references, PAGE_URL_FUNCTION, OctoberThemeFileType.PAGE);

        references.sort(Comparator.comparingInt(reference -> reference.rangeInFile().getStartOffset()));
        return references;
    }

    private static void scanLayoutReferences(@NotNull String fileText, List<Reference> references) {
        Matcher sectionMatcher = SECTION_SEPARATOR.matcher(fileText);
        int configurationEndOffset = sectionMatcher.find() ? sectionMatcher.start() : fileText.length();

        Matcher matcher = LAYOUT_SETTING.matcher(fileText.substring(0, configurationEndOffset));
        while (matcher.find()) {
            references.add(reference(OctoberThemeFileType.LAYOUT, matcher));
        }
    }

    private static void scanReferences(
        @NotNull String fileText,
        List<Reference> references,
        Pattern pattern,
        OctoberThemeFileType type
    ) {
        Matcher matcher = pattern.matcher(fileText);
        while (matcher.find()) {
            references.add(reference(type, matcher));
        }
    }

    private static Reference reference(OctoberThemeFileType type, Matcher matcher) {
        return new Reference(
            type,
            matcher.group(2),
            TextRange.create(matcher.start(2), matcher.end(2))
        );
    }

    public record Reference(
        @NotNull OctoberThemeFileType type,
        @NotNull String name,
        @NotNull TextRange rangeInFile
    ) {
    }
}
