package io.github.itech_framework.code_editor.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class LineOpsLogicTest {

    private static String apply(String text, LineOpsLogic.Edit edit) {
        return text.substring(0, edit.start()) + edit.text() + text.substring(edit.end());
    }

    private static LineOpsLogic.Edit edit(int start, int end, String text, int selStart, int selEnd) {
        return new LineOpsLogic.Edit(start, end, text, selStart, selEnd);
    }

    @Test
    void deleteLinesInTheMiddleKeepsColumn() {
        String text = "a\nbb\nccc";
        LineOpsLogic.Edit result = LineOpsLogic.deleteLines(text, 3, 3);
        assertEquals(edit(2, 5, "", 3, 3), result);
        assertEquals("a\nccc", apply(text, result));
    }

    @Test
    void deleteLastLineRemovesPrecedingNewline() {
        String text = "a\nbb";
        LineOpsLogic.Edit result = LineOpsLogic.deleteLines(text, 3, 3);
        assertEquals(edit(1, 4, "", 1, 1), result);
        assertEquals("a", apply(text, result));
    }

    @Test
    void deleteOnlyLineEmptiesDocument() {
        String text = "abc";
        LineOpsLogic.Edit result = LineOpsLogic.deleteLines(text, 1, 1);
        assertEquals(edit(0, 3, "", 0, 0), result);
        assertEquals("", apply(text, result));
    }

    @Test
    void deleteInEmptyDocumentDoesNothing() {
        assertNull(LineOpsLogic.deleteLines("", 0, 0));
    }

    @Test
    void deleteSelectionIgnoresTrailingNewlineOfSelection() {
        String text = "a\nb\nc\nd";
        assertEquals(edit(2, 6, "", 2, 2), LineOpsLogic.deleteLines(text, 2, 5));
        assertEquals(edit(2, 6, "", 2, 2), LineOpsLogic.deleteLines(text, 2, 6));
        assertEquals("a\nd", apply(text, LineOpsLogic.deleteLines(text, 2, 6)));
    }

    @Test
    void deleteClampsColumnToShorterNextLine() {
        String text = "abcdef\nxy";
        LineOpsLogic.Edit result = LineOpsLogic.deleteLines(text, 5, 5);
        assertEquals(edit(0, 7, "", 2, 2), result);
        assertEquals("xy", apply(text, result));
    }

    @Test
    void duplicateDownMovesCaretToTheCopy() {
        String text = "ab\ncd";
        LineOpsLogic.Edit result = LineOpsLogic.duplicateDown(text, 1, 1);
        assertEquals(edit(2, 2, "\nab", 4, 4), result);
        assertEquals("ab\nab\ncd", apply(text, result));
    }

    @Test
    void duplicateUpKeepsCaretOnTheOriginalPosition() {
        String text = "ab\ncd";
        LineOpsLogic.Edit result = LineOpsLogic.duplicateUp(text, 1, 1);
        assertEquals(edit(0, 0, "ab\n", 1, 1), result);
        assertEquals("ab\nab\ncd", apply(text, result));
    }

    @Test
    void duplicateDownCopiesAllSelectedLines() {
        String text = "a\nb\nc";
        LineOpsLogic.Edit result = LineOpsLogic.duplicateDown(text, 0, 3);
        assertEquals(edit(3, 3, "\na\nb", 4, 7), result);
        assertEquals("a\nb\na\nb\nc", apply(text, result));
    }

    @Test
    void moveUpSwapsWithPreviousLine() {
        String text = "a\nbb\nc";
        LineOpsLogic.Edit result = LineOpsLogic.moveUp(text, 3, 3);
        assertEquals(edit(0, 4, "bb\na", 1, 1), result);
        assertEquals("bb\na\nc", apply(text, result));
    }

    @Test
    void moveUpMovesSelectedBlock() {
        String text = "a\nb\nc";
        LineOpsLogic.Edit result = LineOpsLogic.moveUp(text, 2, 3);
        assertEquals(edit(0, 3, "b\na", 0, 1), result);
        assertEquals("b\na\nc", apply(text, result));
    }

    @Test
    void moveUpOnFirstLineDoesNothing() {
        assertNull(LineOpsLogic.moveUp("a\nb", 0, 0));
    }

    @Test
    void moveDownSwapsWithNextLine() {
        String text = "a\nbb\nc";
        LineOpsLogic.Edit result = LineOpsLogic.moveDown(text, 0, 0);
        assertEquals(edit(0, 4, "bb\na", 3, 3), result);
        assertEquals("bb\na\nc", apply(text, result));
    }

    @Test
    void moveDownOnLastLineDoesNothing() {
        assertNull(LineOpsLogic.moveDown("a\nb", 2, 2));
    }

    @Test
    void commentsEveryLineOfSelection() {
        String text = "a\nb";
        LineOpsLogic.Edit result = LineOpsLogic.toggleComment(text, 0, 3, "//");
        assertEquals(edit(0, 3, "// a\n// b", 0, 9), result);
        assertEquals("// a\n// b", apply(text, result));
    }

    @Test
    void uncommentsWhenEveryLineIsCommented() {
        String text = "// a\n// b";
        LineOpsLogic.Edit result = LineOpsLogic.toggleComment(text, 0, 9, "//");
        assertEquals(edit(0, 9, "a\nb", 0, 3), result);
    }

    @Test
    void uncommentsWithoutSpaceAfterPrefix() {
        assertEquals(edit(0, 3, "a", 1, 1), LineOpsLogic.toggleComment("//a", 3, 3, "//"));
    }

    @Test
    void commentMovesCaretAfterInsertedPrefix() {
        assertEquals(edit(0, 5, "  // foo", 7, 7), LineOpsLogic.toggleComment("  foo", 4, 4, "//"));
        assertEquals(edit(0, 5, "  // foo", 5, 5), LineOpsLogic.toggleComment("  foo", 2, 2, "//"));
    }

    @Test
    void blankLinesAreSkippedWhenOthersHaveText() {
        assertEquals(edit(0, 4, "// a\n\n// b", 0, 10), LineOpsLogic.toggleComment("a\n\nb", 0, 4, "//"));
    }

    @Test
    void commentsAtTheSmallestIndent() {
        assertEquals(edit(0, 9, "  //   a\n  // b", 0, 15), LineOpsLogic.toggleComment("    a\n  b", 0, 9, "//"));
    }

    @Test
    void mixedLinesGetCommentedAgain() {
        assertEquals(edit(0, 6, "// // a\n// b", 0, 12), LineOpsLogic.toggleComment("// a\nb", 0, 6, "//"));
    }

    @Test
    void commentsAnEmptyDocumentLine() {
        assertEquals(edit(0, 0, "// ", 3, 3), LineOpsLogic.toggleComment("", 0, 0, "//"));
    }

    @Test
    void usesTheGivenPrefix() {
        assertEquals(edit(0, 1, "# a", 2, 2), LineOpsLogic.toggleComment("a", 0, 0, "#"));
    }
}