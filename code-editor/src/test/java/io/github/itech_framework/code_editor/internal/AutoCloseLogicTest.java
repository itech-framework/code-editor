package io.github.itech_framework.code_editor.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.itech_framework.code_editor.language.BracketPair;

class AutoCloseLogicTest {

    private static final List<BracketPair> PAIRS = BracketPair.DEFAULT_AUTO_CLOSING;

    private static AutoCloseLogic.Result typed(char c, String line, int column) {
        return AutoCloseLogic.typed(c, line, column, false, PAIRS);
    }

    private static AutoCloseLogic.Result result(AutoCloseLogic.Kind kind, char close) {
        return new AutoCloseLogic.Result(kind, close);
    }

    @Test
    void closesABracketAtTheEndOfTheLine() {
        assertEquals(result(AutoCloseLogic.Kind.PAIR, ')'), typed('(', "foo", 3));
        assertEquals(result(AutoCloseLogic.Kind.PAIR, ']'), typed('[', "", 0));
        assertEquals(result(AutoCloseLogic.Kind.PAIR, '}'), typed('{', "", 0));
    }

    @Test
    void closesBeforeSpacesAndClosingCharacters() {
        assertEquals(result(AutoCloseLogic.Kind.PAIR, ')'), typed('(', " x", 0));
        assertEquals(result(AutoCloseLogic.Kind.PAIR, ')'), typed('(', ")", 0));
        assertEquals(result(AutoCloseLogic.Kind.PAIR, ')'), typed('(', ";", 0));
    }

    @Test
    void doesNotCloseBeforeAWord() {
        assertEquals(result(AutoCloseLogic.Kind.NONE, '\0'), typed('(', "foo", 0));
    }

    @Test
    void typingTheClosingCharacterOvertypesIt() {
        assertEquals(result(AutoCloseLogic.Kind.OVERTYPE, ')'), typed(')', "()", 1));
        assertEquals(result(AutoCloseLogic.Kind.OVERTYPE, '"'), typed('"', "\"\"", 1));
    }

    @Test
    void closingCharacterWithoutMatchIsPlainTyping() {
        assertEquals(result(AutoCloseLogic.Kind.NONE, '\0'), typed(')', "(", 1));
    }

    @Test
    void selectionIsWrappedByAnOpeningCharacter() {
        assertEquals(result(AutoCloseLogic.Kind.WRAP, ')'), AutoCloseLogic.typed('(', "abc", 0, true, PAIRS));
        assertEquals(result(AutoCloseLogic.Kind.WRAP, '"'), AutoCloseLogic.typed('"', "abc", 0, true, PAIRS));
    }

    @Test
    void selectionIsNotWrappedByOtherCharacters() {
        assertEquals(result(AutoCloseLogic.Kind.NONE, '\0'), AutoCloseLogic.typed('a', "abc", 0, true, PAIRS));
        assertEquals(result(AutoCloseLogic.Kind.NONE, '\0'), AutoCloseLogic.typed(')', "abc", 0, true, PAIRS));
    }

    @Test
    void quotesClosePairAtTheEndOfTheLine() {
        assertEquals(result(AutoCloseLogic.Kind.PAIR, '"'), typed('"', "", 0));
        assertEquals(result(AutoCloseLogic.Kind.PAIR, '\''), typed('\'', "x = ", 4));
    }

    @Test
    void quoteAfterAWordIsAnApostrophe() {
        assertEquals(result(AutoCloseLogic.Kind.NONE, '\0'), typed('\'', "it", 2));
    }

    @Test
    void quoteAfterABackslashIsEscaped() {
        assertEquals(result(AutoCloseLogic.Kind.NONE, '\0'), typed('"', "\\", 1));
    }

    @Test
    void quoteWithAnOpenQuoteBeforeItIsNotPaired() {
        assertEquals(result(AutoCloseLogic.Kind.NONE, '\0'), typed('"', "\"a ", 3));
    }

    @Test
    void noPairsMeansNoAutoClosing() {
        assertEquals(result(AutoCloseLogic.Kind.NONE, '\0'), AutoCloseLogic.typed('(', "", 0, false, List.of()));
    }

    @Test
    void backspaceDeletesAnEmptyPair() {
        assertTrue(AutoCloseLogic.deletesPair("()", 1, PAIRS));
        assertTrue(AutoCloseLogic.deletesPair("a[]b", 2, PAIRS));
        assertTrue(AutoCloseLogic.deletesPair("\"\"", 1, PAIRS));
    }

    @Test
    void backspaceKeepsNonEmptyPairs() {
        assertFalse(AutoCloseLogic.deletesPair("(a)", 1, PAIRS));
        assertFalse(AutoCloseLogic.deletesPair("(a)", 2, PAIRS));
    }

    @Test
    void backspaceAtTheEdgesDoesNothing() {
        assertFalse(AutoCloseLogic.deletesPair("()", 0, PAIRS));
        assertFalse(AutoCloseLogic.deletesPair("()", 2, PAIRS));
        assertFalse(AutoCloseLogic.deletesPair("", 0, PAIRS));
    }

    @Test
    void backspaceKeepsAQuotePairInsideAnOpenString() {
        assertFalse(AutoCloseLogic.deletesPair("\"\"\"", 2, PAIRS));
    }

    @Test
    void backspaceDoesNotMatchDifferentBrackets() {
        assertFalse(AutoCloseLogic.deletesPair("(]", 1, PAIRS));
    }
}