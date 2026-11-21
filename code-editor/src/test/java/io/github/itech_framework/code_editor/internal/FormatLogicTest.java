package io.github.itech_framework.code_editor.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import io.github.itech_framework.code_editor.TextEdit;

class FormatLogicTest {

    private static String apply(String text, TextEdit edit) {
        return text.substring(0, edit.start()) + edit.text() + text.substring(edit.end());
    }

    private static void assertRoundTrip(String oldText, String newText) {
        TextEdit edit = FormatLogic.diff(oldText, newText);
        assertEquals(newText, apply(oldText, edit));
    }

    @Test
    void diffOfEqualTextsIsNull() {
        assertNull(FormatLogic.diff("abc", "abc"));
        assertNull(FormatLogic.diff("", ""));
    }

    @Test
    void diffFindsTheChangedMiddle() {
        assertEquals(new TextEdit(1, 2, "X"), FormatLogic.diff("abc", "aXc"));
    }

    @Test
    void diffFindsAnInsertion() {
        assertEquals(new TextEdit(1, 1, "b"), FormatLogic.diff("ac", "abc"));
    }

    @Test
    void diffFindsADeletion() {
        assertEquals(new TextEdit(1, 2, ""), FormatLogic.diff("abc", "ac"));
    }

    @Test
    void diffHandlesEmptyTexts() {
        assertEquals(new TextEdit(0, 0, "ab"), FormatLogic.diff("", "ab"));
        assertEquals(new TextEdit(0, 2, ""), FormatLogic.diff("ab", ""));
    }

    @Test
    void diffNeverSplitsASurrogatePair() {
        TextEdit edit = FormatLogic.diff("a\uD83D\uDE00b", "a\uD83D\uDE01b");
        assertEquals(new TextEdit(1, 3, "\uD83D\uDE01"), edit);
    }

    @Test
    void diffAppliesBackToTheNewText() {
        assertRoundTrip("{\"a\":1}", "{\n  \"a\": 1\n}");
        assertRoundTrip("aaa", "aaaa");
        assertRoundTrip("aaaa", "aaa");
        assertRoundTrip("abcabc", "abc");
        assertRoundTrip("x", "y");
        assertRoundTrip("one\ntwo\n", "one\n\ntwo\n");
    }

    @Test
    void offsetsBeforeTheChangeStayTheSame() {
        String oldText = "{\"a\":1}";
        String newText = "{\n  \"a\": 1\n}";
        assertEquals(0, FormatLogic.mapOffset(oldText, newText, 0));
        assertEquals(1, FormatLogic.mapOffset(oldText, newText, 1));
    }

    @Test
    void offsetsAfterTheChangeShiftByTheLengthDifference() {
        String oldText = "{\"a\":1}";
        String newText = "{\n  \"a\": 1\n}";
        assertEquals(11, FormatLogic.mapOffset(oldText, newText, 6));
        assertEquals(12, FormatLogic.mapOffset(oldText, newText, 7));
    }

    @Test
    void offsetsInsideTheChangeFollowTheSameCharacter() {
        String oldText = "{\"a\":1}";
        String newText = "{\n  \"a\": 1\n}";
        assertEquals(6, FormatLogic.mapOffset(oldText, newText, 3));
        assertEquals(9, FormatLogic.mapOffset(oldText, newText, 5));
    }

    @Test
    void offsetBeforeALettersAfterRemovedSpaceLandsBeforeIt() {
        assertEquals(2, FormatLogic.mapOffset("a  b", "a b", 3));
    }

    @Test
    void offsetInsideRemovedIndentationMovesBeforeTheText() {
        assertEquals(2, FormatLogic.mapOffset("x\n    y", "x\ny", 4));
    }

    @Test
    void offsetInsideRemovedTrailingSpacesStaysAfterTheText() {
        assertEquals(2, FormatLogic.mapOffset("ab  \ncd", "ab\ncd", 3));
    }

    @Test
    void offsetsAreClamped() {
        String oldText = "{\"a\":1}";
        String newText = "{\n  \"a\": 1\n}";
        assertEquals(0, FormatLogic.mapOffset(oldText, newText, -5));
        assertEquals(12, FormatLogic.mapOffset(oldText, newText, 999));
    }

    @Test
    void offsetsOfIdenticalTextsDoNotMove() {
        assertEquals(2, FormatLogic.mapOffset("abc", "abc", 2));
    }
}