package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public final class OctoberComponentPropertyResolver {
    private static final String DEFINE_PROPERTIES_METHOD = "defineProperties";
    private static final Pattern RAW_DEFAULT_VALUE = Pattern.compile("(?i)(true|false|null|[-+]?\\d+(?:\\.\\d+)?)");

    private OctoberComponentPropertyResolver() {
    }

    public static @NotNull List<OctoberComponentProperty> listProperties(
        Path currentTemplatePath,
        String componentAlias
    ) {
        if (currentTemplatePath == null || componentAlias == null || componentAlias.isBlank()) {
            return List.of();
        }

        Optional<Path> componentPath = OctoberComponentResolver.findComponent(currentTemplatePath, componentAlias);
        if (componentPath.isEmpty()) {
            return List.of();
        }

        try {
            return listPropertiesFromSource(Files.readString(componentPath.get()));
        }
        catch (IOException ignored) {
            return List.of();
        }
    }

    static @NotNull List<OctoberComponentProperty> listPropertiesFromSource(@NotNull String source) {
        Optional<String> methodBody = OctoberPhpMethodBodyExtractor.findMethodBody(source, DEFINE_PROPERTIES_METHOD);
        if (methodBody.isEmpty()) {
            return List.of();
        }

        int arrayOpenOffset = findReturnedArrayOpenOffset(methodBody.get());
        if (arrayOpenOffset < 0) {
            return List.of();
        }

        return collectTopLevelEntries(methodBody.get(), arrayOpenOffset).stream()
            .map(entry -> {
                PropertyMetadata metadata = extractMetadata(entry.valueText());
                return new OctoberComponentProperty(
                    entry.key(),
                    metadata.title(),
                    metadata.type(),
                    metadata.defaultValue().value(),
                    metadata.defaultValue().quoted()
                );
            })
            .toList();
    }

    private static int findReturnedArrayOpenOffset(String methodBody) {
        int returnOffset = methodBody.indexOf("return");
        while (returnOffset >= 0) {
            int offset = skipWhitespace(methodBody, returnOffset + "return".length());
            if (offset < methodBody.length() && methodBody.charAt(offset) == '[') {
                return offset;
            }

            if (startsWithWord(methodBody, offset, "array")) {
                int openOffset = skipWhitespace(methodBody, offset + "array".length());
                if (openOffset < methodBody.length() && methodBody.charAt(openOffset) == '(') {
                    return openOffset;
                }
            }

            returnOffset = methodBody.indexOf("return", returnOffset + "return".length());
        }

        return -1;
    }

    private static List<ArrayEntry> collectTopLevelEntries(String text, int arrayOpenOffset) {
        List<ArrayEntry> entries = new ArrayList<>();
        List<Character> stack = new ArrayList<>();
        for (int offset = arrayOpenOffset; offset < text.length(); offset++) {
            char current = text.charAt(offset);
            char next = offset + 1 < text.length() ? text.charAt(offset + 1) : '\0';

            if (current == '/' && next == '/') {
                offset = skipLineComment(text, offset + 2);
                continue;
            }
            if (current == '#') {
                offset = skipLineComment(text, offset + 1);
                continue;
            }
            if (current == '/' && next == '*') {
                offset = skipBlockComment(text, offset + 2);
                continue;
            }

            if (current == '\'' || current == '"') {
                StringLiteral literal = readStringLiteral(text, offset);
                if (stack.size() == 1) {
                    int afterLiteral = skipWhitespace(text, literal.endOffset());
                    if (text.startsWith("=>", afterLiteral) && !literal.value().isBlank()) {
                        int valueStart = skipWhitespace(text, afterLiteral + "=>".length());
                        int valueEnd = findTopLevelValueEnd(text, valueStart);
                        entries.add(new ArrayEntry(literal.value(), text.substring(valueStart, valueEnd).trim()));
                        offset = valueEnd - 1;
                        continue;
                    }
                }
                offset = literal.endOffset() - 1;
                continue;
            }

            if (current == '[' || current == '(') {
                stack.add(matchingClose(current));
                continue;
            }

            if (!stack.isEmpty() && current == stack.getLast()) {
                stack.removeLast();
                if (stack.isEmpty()) {
                    return entries;
                }
            }
        }

        return entries;
    }

    private static int findTopLevelValueEnd(String text, int valueStart) {
        List<Character> stack = new ArrayList<>();
        for (int offset = valueStart; offset < text.length(); offset++) {
            char current = text.charAt(offset);
            char next = offset + 1 < text.length() ? text.charAt(offset + 1) : '\0';

            if (current == '/' && next == '/') {
                offset = skipLineComment(text, offset + 2);
                continue;
            }
            if (current == '#') {
                offset = skipLineComment(text, offset + 1);
                continue;
            }
            if (current == '/' && next == '*') {
                offset = skipBlockComment(text, offset + 2);
                continue;
            }

            if (current == '\'' || current == '"') {
                offset = readStringLiteral(text, offset).endOffset() - 1;
                continue;
            }

            if (current == '[' || current == '(') {
                stack.add(matchingClose(current));
                continue;
            }

            if (!stack.isEmpty() && current == stack.getLast()) {
                stack.removeLast();
                continue;
            }

            if (stack.isEmpty() && (current == ',' || current == ']' || current == ')')) {
                return offset;
            }
        }

        return text.length();
    }

    private static PropertyMetadata extractMetadata(String propertyValueText) {
        int arrayOpenOffset = findArrayOpenOffset(propertyValueText);
        if (arrayOpenOffset < 0) {
            return PropertyMetadata.empty();
        }

        String title = null;
        String type = null;
        DefaultValue defaultValue = DefaultValue.none();
        for (ArrayEntry entry : collectTopLevelEntries(propertyValueText, arrayOpenOffset)) {
            switch (entry.key()) {
                case "title" -> title = parseStringValue(entry.valueText()).orElse(null);
                case "type" -> type = parseStringValue(entry.valueText()).orElse(null);
                case "default" -> defaultValue = parseDefaultValue(entry.valueText());
                default -> {
                }
            }
        }

        return new PropertyMetadata(title, type, defaultValue);
    }

    private static int findArrayOpenOffset(String text) {
        int offset = skipWhitespace(text, 0);
        if (offset < text.length() && text.charAt(offset) == '[') {
            return offset;
        }

        if (startsWithWord(text, offset, "array")) {
            int openOffset = skipWhitespace(text, offset + "array".length());
            if (openOffset < text.length() && text.charAt(openOffset) == '(') {
                return openOffset;
            }
        }

        return -1;
    }

    private static DefaultValue parseDefaultValue(String valueText) {
        String trimmed = valueText.trim();
        if (trimmed.isEmpty()) {
            return DefaultValue.none();
        }

        char first = trimmed.charAt(0);
        if (first == '\'' || first == '"') {
            return new DefaultValue(readStringLiteral(trimmed, 0).value(), true);
        }

        int endOffset = 0;
        while (endOffset < trimmed.length() && !Character.isWhitespace(trimmed.charAt(endOffset))) {
            endOffset++;
        }

        String rawValue = trimmed.substring(0, endOffset);
        if (RAW_DEFAULT_VALUE.matcher(rawValue).matches()) {
            return new DefaultValue(rawValue, false);
        }

        return DefaultValue.none();
    }

    private static Optional<String> parseStringValue(String valueText) {
        String trimmed = valueText.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        char first = trimmed.charAt(0);
        if (first == '\'' || first == '"') {
            return Optional.of(readStringLiteral(trimmed, 0).value());
        }

        return Optional.empty();
    }

    private static StringLiteral readStringLiteral(String text, int quoteOffset) {
        char quote = text.charAt(quoteOffset);
        StringBuilder value = new StringBuilder();
        for (int offset = quoteOffset + 1; offset < text.length(); offset++) {
            char current = text.charAt(offset);
            if (current == '\\') {
                if (offset + 1 < text.length()) {
                    value.append(text.charAt(offset + 1));
                    offset++;
                }
                continue;
            }

            if (current == quote) {
                return new StringLiteral(value.toString(), offset + 1);
            }

            value.append(current);
        }

        return new StringLiteral(value.toString(), text.length());
    }

    private static int skipWhitespace(String text, int offset) {
        int current = offset;
        while (current < text.length() && Character.isWhitespace(text.charAt(current))) {
            current++;
        }
        return current;
    }

    private static int skipLineComment(String text, int offset) {
        int newlineOffset = text.indexOf('\n', offset);
        return newlineOffset < 0 ? text.length() : newlineOffset;
    }

    private static int skipBlockComment(String text, int offset) {
        int endOffset = text.indexOf("*/", offset);
        return endOffset < 0 ? text.length() : endOffset + 1;
    }

    private static boolean startsWithWord(String text, int offset, String word) {
        if (!text.startsWith(word, offset)) {
            return false;
        }

        int endOffset = offset + word.length();
        return endOffset >= text.length() || !Character.isJavaIdentifierPart(text.charAt(endOffset));
    }

    private static char matchingClose(char open) {
        return open == '[' ? ']' : ')';
    }

    private record StringLiteral(@NotNull String value, int endOffset) {
    }

    private record ArrayEntry(@NotNull String key, @NotNull String valueText) {
    }

    private record DefaultValue(@Nullable String value, boolean quoted) {
        static DefaultValue none() {
            return new DefaultValue(null, true);
        }
    }

    private record PropertyMetadata(
        @Nullable String title,
        @Nullable String type,
        @NotNull DefaultValue defaultValue
    ) {
        static PropertyMetadata empty() {
            return new PropertyMetadata(null, null, DefaultValue.none());
        }
    }
}
