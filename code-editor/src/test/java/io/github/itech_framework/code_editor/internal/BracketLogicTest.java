package io.github.itech_framework.code_editor.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.function.IntPredicate;

import org.junit.jupiter.api.Test;

class BracketLogicTest {

    private static final IntPredicate NONE = index -> false;

    private static void assertMatch(String text, int caret, int open, int close) {
        assertArrayEquals(new int[] { open, close }, BracketLogic.match(text, caret, NONE));
    }

    private static void assertNoMatch(String text, int caret) {
        assertNull(BracketLogic.match(text, caret, NONE));
    }

    @Test
    void openingBracketBeforeTheCaretMatchesForward() {
        assertMatch("(a)", 1, 0, 2);
    }

    @Test
    void closingBracketBeforeTheCaretMatchesBackward() {
        assertMatch("(a)", 3, 0, 2);
    }

    @Test
    void bracketAfterTheCaretIsUsedWhenNothingIsBefore() {
        assertMatch("(a)", 0, 0, 2);
    }

    @Test
    void allThreeBracketKinds() {
        assertMatch("[a]", 1, 0, 2);
        assertMatch("{a}", 1, 0, 2);
        assertMatch("{a}", 3, 0, 2);
    }

    @Test
    void bracketBeforeTheCaretWinsOverTheOneAfter() {
        assertMatch("(a)(b)", 3, 0, 2);
    }

    @Test
    void rightBracketIsUsedWhenTheLeftOneHasNoPartner() {
        assertMatch("a(b)", 1, 1, 3);
        assertMatch("x)(y)", 2, 2, 4);
    }

    @Test
    void nestedBracketsMatchTheirOwnPartner() {
        assertMatch("((a))", 1, 0, 4);
        assertMatch("((a))", 2, 1, 3);
        assertMatch("((a))", 4, 1, 3);
        assertMatch("((a))", 5, 0, 4);
    }

    @Test
    void otherBracketKindsDoNotChangeTheDepth() {
        assertMatch("([)]", 1, 0, 2);
    }

    @Test
    void unmatchedBracketGivesNothing() {
        assertNoMatch("(a", 1);
        assertNoMatch("a)", 2);
    }

    @Test
    void noBracketNearTheCaretGivesNothing() {
        assertNoMatch("abc", 1);
        assertNoMatch("", 0);
    }

    @Test
    void caretOutsideTheTextGivesNothing() {
        assertNoMatch("(a)", 99);
        assertNoMatch("(a)", -1);
    }

    @Test
    void ignoredBracketsAreSkippedWhenScanning() {
        IntPredicate skipThird = index -> index == 2;
        assertArrayEquals(new int[] { 0, 3 }, BracketLogic.match("(a))", 1, skipThird));
    }

    @Test
    void ignoredStartingBracketGivesNothing() {
        IntPredicate skipFirst = index -> index == 0;
        assertNull(BracketLogic.match("(a)", 1, skipFirst));
    }

    @Test
    void forwardScanStopsAtTheLimit() {
        String near = "(" + "a".repeat(BracketLogic.MAX_SCAN - 1) + ")";
        assertMatch(near, 1, 0, BracketLogic.MAX_SCAN);
        String far = "(" + "a".repeat(BracketLogic.MAX_SCAN) + ")";
        assertNoMatch(far, 1);
    }

    @Test
    void backwardScanStopsAtTheLimit() {
        String near = "(" + "a".repeat(BracketLogic.MAX_SCAN - 1) + ")";
        assertMatch(near, near.length(), 0, BracketLogic.MAX_SCAN);
        String far = "(" + "a".repeat(BracketLogic.MAX_SCAN) + ")";
        assertNoMatch(far, far.length());
    }
}