package io.github.itech_framework.code_editor.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IndentLogicTest {

    @Test
    void keepsTheIndentOfTheCurrentLine() {
        assertEquals(new IndentLogic.Result("\n  ", 3, 0), IndentLogic.enter("  foo", 5, "", "    "));
    }

    @Test
    void addsOneUnitAfterAnOpeningBracket() {
        assertEquals(new IndentLogic.Result("\n      ", 7, 0), IndentLogic.enter("  if (x) {", 10, "", "    "));
    }

    @Test
    void ignoresTrailingSpacesBeforeTheCaret() {
        assertEquals(new IndentLogic.Result("\n  ", 3, 0), IndentLogic.enter("{  ", 3, "", "  "));
    }

    @Test
    void splitsAMatchingPairOverThreeLines() {
        assertEquals(new IndentLogic.Result("\n  \n", 3, 0), IndentLogic.enter("{}", 1, "}", "  "));
    }

    @Test
    void consumesSpacesBeforeTheClosingBracket() {
        assertEquals(new IndentLogic.Result("\n\t\n", 2, 1), IndentLogic.enter("(", 1, " )", "\t"));
    }

    @Test
    void doesNotSplitWhenTheClosingBracketDoesNotMatch() {
        assertEquals(new IndentLogic.Result("\n  ", 3, 0), IndentLogic.enter("[", 1, ")", "  "));
    }

    @Test
    void usesOnlyTheIndentBeforeTheCaret() {
        assertEquals(new IndentLogic.Result("\n  ", 3, 0), IndentLogic.enter("    x", 2, "  x", "  "));
    }

    @Test
    void keepsTabsInTheIndent() {
        assertEquals(new IndentLogic.Result("\n\t\t", 3, 0), IndentLogic.enter("\tfoo(", 5, "", "\t"));
    }
}