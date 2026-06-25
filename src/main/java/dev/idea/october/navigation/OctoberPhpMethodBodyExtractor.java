package dev.idea.october.navigation;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class OctoberPhpMethodBodyExtractor {
    private OctoberPhpMethodBodyExtractor() {
    }

    static @NotNull Optional<String> findMethodBody(@NotNull String source, @NotNull String methodName) {
        Pattern declaration = Pattern.compile("\\bfunction\\s+" + Pattern.quote(methodName) + "\\s*\\(");
        Matcher matcher = declaration.matcher(source);
        if (!matcher.find()) {
            return Optional.empty();
        }

        int openBrace = source.indexOf('{', matcher.end());
        if (openBrace < 0) {
            return Optional.empty();
        }

        int closeBrace = findMatchingBrace(source, openBrace);
        if (closeBrace < 0) {
            return Optional.empty();
        }

        return Optional.of(source.substring(openBrace + 1, closeBrace));
    }

    private static int findMatchingBrace(@NotNull String source, int openBrace) {
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean escaped = false;

        for (int offset = openBrace; offset < source.length(); offset++) {
            char current = source.charAt(offset);
            char next = offset + 1 < source.length() ? source.charAt(offset + 1) : '\0';

            if (inLineComment) {
                if (current == '\n' || current == '\r') {
                    inLineComment = false;
                }
                continue;
            }

            if (inBlockComment) {
                if (current == '*' && next == '/') {
                    inBlockComment = false;
                    offset++;
                }
                continue;
            }

            if (inSingleQuote || inDoubleQuote) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (current == '\\') {
                    escaped = true;
                    continue;
                }
                if (inSingleQuote && current == '\'') {
                    inSingleQuote = false;
                }
                else if (inDoubleQuote && current == '"') {
                    inDoubleQuote = false;
                }
                continue;
            }

            if (current == '/' && next == '/') {
                inLineComment = true;
                offset++;
                continue;
            }
            if (current == '#') {
                inLineComment = true;
                continue;
            }
            if (current == '/' && next == '*') {
                inBlockComment = true;
                offset++;
                continue;
            }
            if (current == '\'') {
                inSingleQuote = true;
                continue;
            }
            if (current == '"') {
                inDoubleQuote = true;
                continue;
            }

            if (current == '{') {
                depth++;
            }
            else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return offset;
                }
            }
        }

        return -1;
    }
}
