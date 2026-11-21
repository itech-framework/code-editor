package io.github.itech_framework.code_editor.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.IntPredicate;

import org.junit.jupiter.api.Test;

import io.github.itech_framework.code_editor.internal.FoldLogic.Range;
import io.github.itech_framework.code_editor.internal.FoldLogic.Scan;

class FoldLogicTest {

    private static final IntPredicate NONE = index -> false;

    private static Range r(int start, int end) {
        return new Range(start, end);
    }

    private static List<Range> sample() {
        return List.of(r(0, 10), r(2, 6), r(3, 5), r(12, 20));
    }

    private static IntPredicate lines(Integer... hidden) {
        List<Integer> set = List.of(hidden);
        return set::contains;
    }

    @Test
    void rangeHidesTheLinesBetweenTheBrackets() {
        Range range = r(2, 6);
        assertEquals(3, range.firstHiddenLine());
        assertEquals(5, range.lastHiddenLine());
    }

    @Test
    void blockOfThreeLinesFolds() {
        assertEquals(List.of(r(0, 2)), FoldLogic.compute("{\n  a\n}", NONE));
        assertEquals(List.of(r(0, 3)), FoldLogic.compute("{\n a\n b\n}", NONE));
    }

    @Test
    void squareBracketsFoldToo() {
        assertEquals(List.of(r(0, 2)), FoldLogic.compute("[\n a\n]", NONE));
    }

    @Test
    void shortBlocksDoNotFold() {
        assertTrue(FoldLogic.compute("{\n}", NONE).isEmpty());
        assertTrue(FoldLogic.compute("{ a }", NONE).isEmpty());
        assertTrue(FoldLogic.compute("", NONE).isEmpty());
    }

    @Test
    void roundBracketsDoNotFold() {
        assertTrue(FoldLogic.compute("(\n a\n)", NONE).isEmpty());
    }

    @Test
    void nestedBlocksAreSortedByStartLine() {
        assertEquals(List.of(r(0, 4), r(1, 3)), FoldLogic.compute("{\n  [\n    a\n  ]\n}", NONE));
    }

    @Test
    void blocksStartingOnTheSameLineKeepTheLargerRange() {
        assertEquals(List.of(r(0, 4)), FoldLogic.compute("{ [\n a\n ]\n b\n}", NONE));
    }

    @Test
    void mismatchedOrUnmatchedBracketsGiveNoRange() {
        assertTrue(FoldLogic.compute("{\n a\n]\n", NONE).isEmpty());
        assertTrue(FoldLogic.compute("}\n\n\n", NONE).isEmpty());
        assertTrue(FoldLogic.compute("{\n a\n b\n", NONE).isEmpty());
    }

    @Test
    void ignoredBracketsAreSkipped() {
        String text = "{\n  a\n  }\n  b\n}";
        int skip = text.indexOf('}');
        assertEquals(List.of(r(0, 2)), FoldLogic.compute(text, NONE));
        assertEquals(List.of(r(0, 4)), FoldLogic.compute(text, index -> index == skip));
    }

    @Test
    void carriageReturnsDoNotCountAsLines() {
        assertEquals(List.of(r(0, 2)), FoldLogic.compute("{\r\n a\r\n}", NONE));
    }

    @Test
    void nestingAtTheDepthLimitStillFolds() {
        String text = "{\n".repeat(FoldLogic.MAX_DEPTH) + "}\n".repeat(FoldLogic.MAX_DEPTH);
        List<Range> ranges = FoldLogic.compute(text, NONE);
        assertEquals(FoldLogic.MAX_DEPTH - 1, ranges.size());
        assertEquals(r(0, 2 * FoldLogic.MAX_DEPTH - 1), ranges.get(0));
    }

    @Test
    void nestingBeyondTheDepthLimitStopsTheScan() {
        String text = "{\n".repeat(FoldLogic.MAX_DEPTH + 1) + "}\n".repeat(FoldLogic.MAX_DEPTH + 1);
        assertTrue(FoldLogic.compute(text, NONE).isEmpty());
    }

    @Test
    void innermostPicksTheDeepestRangeContainingTheLine() {
        List<Range> ranges = sample();
        assertEquals(r(3, 5), FoldLogic.innermost(ranges, 4, range -> true));
        assertEquals(r(0, 10), FoldLogic.innermost(ranges, 1, range -> true));
        assertEquals(r(2, 6), FoldLogic.innermost(ranges, 6, range -> true));
        assertEquals(r(12, 20), FoldLogic.innermost(ranges, 15, range -> true));
    }

    @Test
    void innermostIncludesBothEndLines() {
        List<Range> ranges = sample();
        assertEquals(r(3, 5), FoldLogic.innermost(ranges, 3, range -> true));
        assertEquals(r(3, 5), FoldLogic.innermost(ranges, 5, range -> true));
    }

    @Test
    void innermostHonoursTheAcceptFilter() {
        assertEquals(r(2, 6), FoldLogic.innermost(sample(), 4, range -> range.startLine() != 3));
    }

    @Test
    void innermostGivesNullWhenNothingContainsTheLine() {
        assertNull(FoldLogic.innermost(sample(), 11, range -> true));
        assertNull(FoldLogic.innermost(List.of(), 0, range -> true));
        assertNull(FoldLogic.innermost(sample(), 4, range -> false));
    }

    @Test
    void startingAtFindsTheRangeByItsFirstLine() {
        List<Range> ranges = sample();
        assertEquals(r(0, 10), FoldLogic.startingAt(ranges, 0));
        assertEquals(r(3, 5), FoldLogic.startingAt(ranges, 3));
        assertEquals(r(12, 20), FoldLogic.startingAt(ranges, 12));
    }

    @Test
    void startingAtGivesNullForOtherLines() {
        List<Range> ranges = sample();
        assertNull(FoldLogic.startingAt(ranges, 1));
        assertNull(FoldLogic.startingAt(ranges, 13));
        assertNull(FoldLogic.startingAt(ranges, -1));
        assertNull(FoldLogic.startingAt(List.of(), 0));
    }

    @Test
    void scanAcceptsHiddenLinesThatMatchTheirRange() {
        Scan scan = FoldLogic.scan(List.of(r(2, 6)), lines(3, 4, 5), 10);
        assertEquals(List.of(), scan.stale());
        assertEquals(1, scan.runs());
    }

    @Test
    void scanAcceptsSeveralFolds() {
        Scan scan = FoldLogic.scan(List.of(r(1, 4), r(6, 9)), lines(2, 3, 7, 8), 10);
        assertEquals(List.of(), scan.stale());
        assertEquals(2, scan.runs());
    }

    @Test
    void scanFlagsARunThatEndsTooEarlyOrTooLate() {
        assertEquals(List.of(2), FoldLogic.scan(List.of(r(2, 6)), lines(3, 4), 10).stale());
        assertEquals(List.of(2), FoldLogic.scan(List.of(r(2, 6)), lines(3, 4, 5, 6), 10).stale());
    }

    @Test
    void scanFlagsARunWithoutARange() {
        Scan scan = FoldLogic.scan(List.of(), lines(3, 4, 5), 10);
        assertEquals(List.of(2), scan.stale());
        assertEquals(1, scan.runs());
    }

    @Test
    void scanFlagsEveryStaleRunInOrder() {
        Scan scan = FoldLogic.scan(List.of(), lines(2, 5), 10);
        assertEquals(List.of(1, 4), scan.stale());
        assertEquals(2, scan.runs());
    }

    @Test
    void scanWithNoHiddenLinesIsEmpty() {
        Scan scan = FoldLogic.scan(sample(), NONE, 10);
        assertEquals(List.of(), scan.stale());
        assertEquals(0, scan.runs());
    }

    @Test
    void scanDoesNotFlagARunThatStartsOnTheFirstLine() {
        Scan scan = FoldLogic.scan(List.of(), lines(0, 1), 10);
        assertEquals(List.of(), scan.stale());
        assertEquals(1, scan.runs());
    }

    @Test
    void scanStopsAtTheLineCount() {
        Scan scan = FoldLogic.scan(List.of(), index -> true, 3);
        assertEquals(List.of(), scan.stale());
        assertEquals(1, scan.runs());
    }

    @Test
    void scanAcceptsAFoldThatEndsAtTheLastLine() {
        Scan scan = FoldLogic.scan(List.of(r(2, 6)), lines(3, 4, 5), 6);
        assertEquals(List.of(), scan.stale());
        assertEquals(1, scan.runs());
    }
}