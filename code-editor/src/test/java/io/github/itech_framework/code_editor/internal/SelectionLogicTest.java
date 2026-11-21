package io.github.itech_framework.code_editor.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class SelectionLogicTest {

    private static MultiEditLogic.Span span(int start, int end) {
        return new MultiEditLogic.Span(start, end);
    }

    @Test
    void wordCharactersIncludeLettersDigitsAndUnderscore() {
        assertTrue(SelectionLogic.isWordChar('a'));
        assertTrue(SelectionLogic.isWordChar('Z'));
        assertTrue(SelectionLogic.isWordChar('5'));
        assertTrue(SelectionLogic.isWordChar('_'));
    }

    @Test
    void wordCharactersExcludeSeparators() {
        assertFalse(SelectionLogic.isWordChar(' '));
        assertFalse(SelectionLogic.isWordChar('-'));
        assertFalse(SelectionLogic.isWordChar('.'));
    }

    @Test
    void wordCharactersIncludeMarksUsedByComplexScripts() {
        assertTrue(SelectionLogic.isWordChar('\u1019'));
        assertTrue(SelectionLogic.isWordChar('\u1031'));
        assertTrue(SelectionLogic.isWordChar('\u0301'));
        assertTrue(SelectionLogic.isWordChar('\u20DD'));
    }

    @Test
    void wordAtFindsTheWordAroundTheOffset() {
        assertEquals(span(0, 5), SelectionLogic.wordAt("hello world", 2));
        assertEquals(span(0, 5), SelectionLogic.wordAt("hello world", 0));
    }

    @Test
    void wordAtPicksTheWordTouchingTheOffset() {
        assertEquals(span(0, 5), SelectionLogic.wordAt("hello world", 5));
        assertEquals(span(6, 11), SelectionLogic.wordAt("hello world", 6));
    }

    @Test
    void wordAtBetweenSeparatorsIsNull() {
        assertNull(SelectionLogic.wordAt("hello  world", 6));
        assertNull(SelectionLogic.wordAt("", 0));
    }

    @Test
    void wordAtClampsTheOffset() {
        assertEquals(span(0, 2), SelectionLogic.wordAt("ab", 99));
        assertEquals(span(0, 2), SelectionLogic.wordAt("ab", -5));
    }

    @Test
    void wordAtKeepsMyanmarSyllablesTogether() {
        assertEquals(span(0, 2), SelectionLogic.wordAt("\u1019\u1031 x", 1));
    }

    @Test
    void nextOccurrenceSearchesForward() {
        String text = "foo bar foo baz foo";
        assertEquals(8, SelectionLogic.nextOccurrence(text, "foo", 1, false, List.of()));
        assertEquals(16, SelectionLogic.nextOccurrence(text, "foo", 9, false, List.of()));
    }

    @Test
    void nextOccurrenceWrapsAround() {
        assertEquals(0, SelectionLogic.nextOccurrence("foo bar foo baz foo", "foo", 17, false, List.of()));
    }

    @Test
    void nextOccurrenceSkipsTakenSpans() {
        assertEquals(16,
                SelectionLogic.nextOccurrence("foo bar foo baz foo", "foo", 1, false, List.of(span(8, 11))));
    }

    @Test
    void nextOccurrenceHonoursWholeWord() {
        assertEquals(9, SelectionLogic.nextOccurrence("foo food foo", "foo", 1, true, List.of()));
        assertEquals(4, SelectionLogic.nextOccurrence("foo food foo", "foo", 1, false, List.of()));
    }

    @Test
    void nextOccurrenceReturnsMinusOneWhenNothingIsLeft() {
        assertEquals(-1, SelectionLogic.nextOccurrence("abc", "x", 0, false, List.of()));
        assertEquals(-1, SelectionLogic.nextOccurrence("abc", "", 0, false, List.of()));
        assertEquals(-1, SelectionLogic.nextOccurrence("ab", "abc", 0, false, List.of()));
        assertEquals(-1, SelectionLogic.nextOccurrence("a a", "a", 0, false, List.of(span(0, 1), span(2, 3))));
    }

    @Test
    void allOccurrencesFindsEveryMatch() {
        assertEquals(List.of(span(0, 1), span(2, 3), span(4, 5)),
                SelectionLogic.allOccurrences("a a a", "a", false, 10));
    }

    @Test
    void allOccurrencesStopsAtTheLimit() {
        assertEquals(List.of(span(0, 1), span(2, 3)), SelectionLogic.allOccurrences("a a a", "a", false, 2));
    }

    @Test
    void allOccurrencesHonoursWholeWord() {
        assertEquals(List.of(span(0, 3), span(9, 12)),
                SelectionLogic.allOccurrences("foo food foo", "foo", true, 10));
    }

    @Test
    void allOccurrencesDoNotOverlap() {
        assertEquals(List.of(span(0, 2), span(2, 4)), SelectionLogic.allOccurrences("aaaa", "aa", false, 10));
    }

    @Test
    void allOccurrencesOfEmptyNeedleIsEmpty() {
        assertEquals(List.of(), SelectionLogic.allOccurrences("abc", "", false, 10));
    }

    @Test
    void lineEndsCoverEverySelectedLine() {
        assertEquals(List.of(2, 5, 8), SelectionLogic.lineEnds("ab\ncd\nef", 0, 8));
        assertEquals(List.of(2, 5), SelectionLogic.lineEnds("ab\ncd\nef", 1, 4));
    }

    @Test
    void lineEndsOfACaretIsOneEntry() {
        assertEquals(List.of(2), SelectionLogic.lineEnds("ab\ncd\nef", 1, 1));
    }

    @Test
    void lineEndsIgnoreATrailingNewlineOfTheSelection() {
        assertEquals(List.of(2), SelectionLogic.lineEnds("ab\ncd\nef", 0, 3));
        assertEquals(List.of(1), SelectionLogic.lineEnds("a\n", 0, 2));
    }

    @Test
    void lineEndsOnTheEmptyLastLine() {
        assertEquals(List.of(2), SelectionLogic.lineEnds("a\n", 2, 2));
    }
}