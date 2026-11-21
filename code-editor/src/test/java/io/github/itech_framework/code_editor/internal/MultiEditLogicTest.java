package io.github.itech_framework.code_editor.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.itech_framework.code_editor.TextEdit;

class MultiEditLogicTest {

    private static MultiEditLogic.Span span(int start, int end) {
        return new MultiEditLogic.Span(start, end);
    }

    private static TextEdit edit(int start, int end, String text) {
        return new TextEdit(start, end, text);
    }

    private static MultiEditLogic.Plan plan(List<TextEdit> edits, List<Integer> carets, int primary) {
        return new MultiEditLogic.Plan(edits, carets, primary);
    }

    @Test
    void spanRejectsInvalidRanges() {
        assertThrows(IllegalArgumentException.class, () -> span(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> span(2, 1));
    }

    @Test
    void insertAtSeveralCarets() {
        MultiEditLogic.Plan result = MultiEditLogic.insert("abcd", List.of(span(1, 1), span(3, 3)), 1, "X");
        assertEquals(plan(List.of(edit(1, 1, "X"), edit(3, 3, "X")), List.of(2, 5), 1), result);
    }

    @Test
    void insertReplacesSelections() {
        MultiEditLogic.Plan result = MultiEditLogic.insert("abcd", List.of(span(0, 2), span(3, 4)), 0, "Z");
        assertEquals(plan(List.of(edit(0, 2, "Z"), edit(3, 4, "Z")), List.of(1, 3), 0), result);
    }

    @Test
    void insertMergesOverlappingSpans() {
        MultiEditLogic.Plan result = MultiEditLogic.insert("abcd", List.of(span(0, 2), span(1, 3)), 1, "Q");
        assertEquals(plan(List.of(edit(0, 3, "Q")), List.of(1), 0), result);
    }

    @Test
    void insertMergesCaretsAtTheSamePosition() {
        MultiEditLogic.Plan result = MultiEditLogic.insert("abcd", List.of(span(1, 1), span(1, 1)), 1, "X");
        assertEquals(plan(List.of(edit(1, 1, "X")), List.of(2), 0), result);
    }

    @Test
    void insertOfEmptyTextOnlyMovesCarets() {
        MultiEditLogic.Plan result = MultiEditLogic.insert("abcd", List.of(span(1, 1), span(3, 3)), 0, "");
        assertEquals(plan(List.of(), List.of(1, 3), 0), result);
    }

    @Test
    void insertSortsSpansGivenOutOfOrder() {
        MultiEditLogic.Plan result = MultiEditLogic.insert("abcd", List.of(span(3, 3), span(1, 1)), 0, "X");
        assertEquals(plan(List.of(edit(1, 1, "X"), edit(3, 3, "X")), List.of(2, 5), 1), result);
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> MultiEditLogic.insert("abc", List.of(), 0, "x"));
        assertThrows(IllegalArgumentException.class,
                () -> MultiEditLogic.insert("abc", List.of(span(0, 0)), 1, "x"));
        assertThrows(IllegalArgumentException.class,
                () -> MultiEditLogic.insert("abc", List.of(span(0, 0)), -1, "x"));
        assertThrows(IllegalArgumentException.class,
                () -> MultiEditLogic.insert("abc", List.of(span(0, 4)), 0, "x"));
    }

    @Test
    void backspaceRemovesTheCharacterBeforeEachCaret() {
        MultiEditLogic.Plan result = MultiEditLogic.backspace("abc", List.of(span(1, 1), span(3, 3)), 0);
        assertEquals(plan(List.of(edit(0, 1, ""), edit(2, 3, "")), List.of(0, 1), 0), result);
    }

    @Test
    void backspaceAtDocumentStartDoesNothing() {
        assertEquals(plan(List.of(), List.of(0), 0), MultiEditLogic.backspace("abc", List.of(span(0, 0)), 0));
    }

    @Test
    void backspaceDeletesSelectionFirst() {
        assertEquals(plan(List.of(edit(1, 3, "")), List.of(1), 0),
                MultiEditLogic.backspace("abcd", List.of(span(1, 3)), 0));
    }

    @Test
    void backspaceRemovesWholeSurrogatePair() {
        assertEquals(plan(List.of(edit(1, 3, "")), List.of(1), 0),
                MultiEditLogic.backspace("a\uD83D\uDE00b", List.of(span(3, 3)), 0));
    }

    @Test
    void deleteRemovesTheCharacterAfterEachCaret() {
        MultiEditLogic.Plan result = MultiEditLogic.delete("abc", List.of(span(0, 0), span(2, 2)), 0);
        assertEquals(plan(List.of(edit(0, 1, ""), edit(2, 3, "")), List.of(0, 1), 0), result);
    }

    @Test
    void deleteAtDocumentEndDoesNothing() {
        assertEquals(plan(List.of(), List.of(3), 0), MultiEditLogic.delete("abc", List.of(span(3, 3)), 0));
    }

    @Test
    void deleteRemovesWholeSurrogatePair() {
        assertEquals(plan(List.of(edit(1, 3, "")), List.of(1), 0),
                MultiEditLogic.delete("a\uD83D\uDE00b", List.of(span(1, 1)), 0));
    }

    @Test
    void enterKeepsIndent() {
        assertEquals(plan(List.of(edit(5, 5, "\n  ")), List.of(8), 0),
                MultiEditLogic.enter("  foo", List.of(span(5, 5)), 0, "  "));
    }

    @Test
    void enterSplitsAnEmptyPair() {
        assertEquals(plan(List.of(edit(1, 1, "\n  \n")), List.of(4), 0),
                MultiEditLogic.enter("{}", List.of(span(1, 1)), 0, "  "));
    }

    @Test
    void enterReplacesSelection() {
        assertEquals(plan(List.of(edit(3, 4, "\n")), List.of(4), 0),
                MultiEditLogic.enter("foo bar", List.of(span(3, 4)), 0, "  "));
    }

    @Test
    void enterIndentsAfterOpeningBracketOnLaterLine() {
        assertEquals(plan(List.of(edit(6, 6, "\n    ")), List.of(11), 0),
                MultiEditLogic.enter("a\n  b{", List.of(span(6, 6)), 0, "  "));
    }

    @Test
    void tabInsertsSpacesToTheNextTabStop() {
        assertEquals(plan(List.of(edit(1, 1, "   ")), List.of(4), 0),
                MultiEditLogic.tab("ab", List.of(span(1, 1)), 0, 4, true));
    }

    @Test
    void tabInsertsTabCharacter() {
        assertEquals(plan(List.of(edit(1, 1, "\t")), List.of(2), 0),
                MultiEditLogic.tab("ab", List.of(span(1, 1)), 0, 4, false));
    }

    @Test
    void tabReplacesSelection() {
        assertEquals(plan(List.of(edit(1, 3, "   ")), List.of(4), 0),
                MultiEditLogic.tab("abcd", List.of(span(1, 3)), 0, 4, true));
    }

    @Test
    void tabAlignsEachCaretOnItsOwnLine() {
        assertEquals(plan(List.of(edit(1, 1, " "), edit(4, 4, "  ")), List.of(2, 7), 0),
                MultiEditLogic.tab("a\nbb", List.of(span(1, 1), span(4, 4)), 0, 2, true));
    }

    @Test
    void outdentRemovesIndentOfEverySelectedLine() {
        assertEquals(plan(List.of(edit(0, 4, ""), edit(6, 8, "")), List.of(3), 0),
                MultiEditLogic.outdent("    a\n  b", List.of(span(0, 9)), 0, 4));
    }

    @Test
    void outdentSkipsTheLineWhereSelectionEndsAtItsStart() {
        assertEquals(plan(List.of(edit(0, 2, "")), List.of(2), 0),
                MultiEditLogic.outdent("  a\n  b", List.of(span(0, 4)), 0, 4));
    }

    @Test
    void outdentWithNothingToRemoveKeepsCarets() {
        assertEquals(plan(List.of(), List.of(1), 0), MultiEditLogic.outdent("abc", List.of(span(1, 1)), 0, 4));
    }

    @Test
    void outdentWithTwoCaretsOnOneLineEditsTheLineOnce() {
        assertEquals(plan(List.of(edit(0, 4, "")), List.of(0, 1), 1),
                MultiEditLogic.outdent("    ab", List.of(span(4, 4), span(5, 5)), 1, 4));
    }

    @Test
    void outdentMovesCaretOutOfRemovedIndent() {
        assertEquals(plan(List.of(edit(0, 4, "")), List.of(0), 0),
                MultiEditLogic.outdent("      a", List.of(span(2, 2)), 0, 4));
    }

    @Test
    void outdentRemovesOneTab() {
        assertEquals(plan(List.of(edit(0, 1, "")), List.of(0), 0),
                MultiEditLogic.outdent("\tx", List.of(span(1, 1)), 0, 4));
    }
}