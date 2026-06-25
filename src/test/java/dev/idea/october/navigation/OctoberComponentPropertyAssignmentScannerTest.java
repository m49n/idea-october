package dev.idea.october.navigation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OctoberComponentPropertyAssignmentScannerTest {
    @Test
    void scansPropertyAssignmentsInsideComponentBlocksOnly() {
        String text = """
            url = "/journal"
            [PressCenterPosts]
            slug = "{{ :slug }}"
            missing = "value"
            [OtherComponent]
            enabled = 1
            ==
            missing = "markup"
            """;

        List<OctoberComponentPropertyAssignmentScanner.Assignment> assignments =
            OctoberComponentPropertyAssignmentScanner.scan(text);

        assertEquals(
            List.of(
                "PressCenterPosts:slug",
                "PressCenterPosts:missing",
                "OtherComponent:enabled"
            ),
            assignments.stream()
                .map(assignment -> assignment.componentAlias() + ":" + assignment.propertyName())
                .toList()
        );
        assertEquals("missing", assignments.get(1).rangeInFile().substring(text));
    }
}
