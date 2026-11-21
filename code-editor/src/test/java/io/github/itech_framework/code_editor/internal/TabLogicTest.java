package io.github.itech_framework.code_editor.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TabLogicTest {

    @Test
    void insertionUsesTabCharacterWhenSpacesAreOff() {
        assertEquals("\t", TabLogic.insertion("abc", 2, 4, false));
    }

    @Test
    void insertionFillsToTheNextTabStop() {
        assertEquals("    ", TabLogic.insertion("", 0, 4, true));
        assertEquals("  ", TabLogic.insertion("ab", 2, 4, true));
        assertEquals("    ", TabLogic.insertion("abcd", 4, 4, true));
    }

    @Test
    void insertionCountsTabsAsVisualWidth() {
        assertEquals("   ", TabLogic.insertion("\tx", 2, 4, true));
    }

    @Test
    void insertionToleratesColumnPastTheEnd() {
        assertEquals("  ", TabLogic.insertion("ab", 10, 4, true));
    }

    @Test
    void indentAddsUnitToEveryLine() {
        TabLogic.Block block = TabLogic.indent("a\nb", 0, 3, "  ");
        assertEquals(new TabLogic.Block("  a\n  b", 0, 7), block);
    }

    @Test
    void indentSkipsEmptyLines() {
        TabLogic.Block block = TabLogic.indent("a\n\nb", 0, 4, "\t");
        assertEquals(new TabLogic.Block("\ta\n\n\tb", 0, 6), block);
    }

    @Test
    void indentKeepsColumnZeroBeforeTheNewIndent() {
        TabLogic.Block block = TabLogic.indent("a\nb", 2, 2, "  ");
        assertEquals(new TabLogic.Block("  a\n  b", 4, 4), block);
    }

    @Test
    void outdentRemovesOneLevelFromEveryLine() {
        TabLogic.Block block = TabLogic.outdent("    a\n\tb", 0, 8, 4);
        assertEquals(new TabLogic.Block("a\nb", 0, 3), block);
    }

    @Test
    void outdentMovesCaretOutOfTheRemovedIndent() {
        TabLogic.Block block = TabLogic.outdent("      a", 2, 2, 4);
        assertEquals(new TabLogic.Block("  a", 0, 0), block);
    }

    @Test
    void removableCountsOneTabOrUpToTabSizeSpaces() {
        assertEquals(0, TabLogic.removable("", 4));
        assertEquals(0, TabLogic.removable("x", 4));
        assertEquals(1, TabLogic.removable("\tx", 4));
        assertEquals(2, TabLogic.removable("  x", 4));
        assertEquals(4, TabLogic.removable("      x", 4));
        assertEquals(1, TabLogic.removable(" \tx", 4));
    }
}