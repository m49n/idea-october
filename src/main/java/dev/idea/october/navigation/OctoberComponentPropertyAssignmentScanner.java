package dev.idea.october.navigation;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OctoberComponentPropertyAssignmentScanner {
    private static final String COMPONENT_IDENTIFIER = "[A-Za-z_][A-Za-z0-9_]*";
    private static final String PROPERTY_IDENTIFIER = "[A-Za-z_][A-Za-z0-9_-]*";
    private static final Pattern COMPONENT_BLOCK = Pattern.compile(
        "^\\s*\\[(" + COMPONENT_IDENTIFIER + ")(?:\\s+(" + COMPONENT_IDENTIFIER + "))?]\\s*$"
    );
    private static final Pattern PROPERTY_ASSIGNMENT = Pattern.compile("^\\s*(" + PROPERTY_IDENTIFIER + ")\\s*=");

    private OctoberComponentPropertyAssignmentScanner() {
    }

    public static @NotNull List<Assignment> scan(@NotNull String fileText) {
        int configEnd = fileText.indexOf("==");
        if (configEnd < 0) {
            configEnd = fileText.length();
        }

        List<Assignment> assignments = new ArrayList<>();
        String currentComponentAlias = null;
        int lineStart = 0;
        while (lineStart < configEnd) {
            int lineEnd = fileText.indexOf('\n', lineStart);
            if (lineEnd < 0 || lineEnd > configEnd) {
                lineEnd = configEnd;
            }

            String line = fileText.substring(lineStart, lineEnd);
            Matcher componentBlockMatcher = COMPONENT_BLOCK.matcher(line);
            if (componentBlockMatcher.matches()) {
                currentComponentAlias = componentBlockMatcher.group(1);
            }
            else if (currentComponentAlias != null) {
                Matcher assignmentMatcher = PROPERTY_ASSIGNMENT.matcher(line);
                if (assignmentMatcher.find()) {
                    assignments.add(new Assignment(
                        currentComponentAlias,
                        assignmentMatcher.group(1),
                        TextRange.create(
                            lineStart + assignmentMatcher.start(1),
                            lineStart + assignmentMatcher.end(1)
                        )
                    ));
                }
            }

            lineStart = lineEnd + 1;
        }

        return assignments;
    }

    public record Assignment(
        @NotNull String componentAlias,
        @NotNull String propertyName,
        @NotNull TextRange rangeInFile
    ) {
    }
}
