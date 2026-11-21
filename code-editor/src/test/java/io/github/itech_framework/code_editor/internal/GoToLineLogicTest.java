package io.github.itech_framework.code_editor.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.itech_framework.code_editor.internal.GoToLineLogic.Result;
import io.github.itech_framework.code_editor.internal.GoToLineLogic.Target;

class GoToLineLogicTest {

    private static final String FORMAT_ERROR = "Enter a line number, or line:column";

    private static void assertTarget(Result result, int line, int column) {
        assertNull(result.error());
        assertEquals(new Target(line, column), result.target());
        assertFalse(result.isEmpty());
    }

    private static void assertError(Result result, String message) {
        assertNull(result.target());
        assertEquals(message, result.error());
        assertFalse(result.isEmpty());
    }

    @Test
    void nullAndBlankInputAreEmpty() {
        assertTrue(GoToLineLogic.parse(null, 10).isEmpty());
        assertTrue(GoToLineLogic.parse("", 10).isEmpty());
        assertTrue(GoToLineLogic.parse("   ", 10).isEmpty());
    }

    @Test
    void lineNumberAloneUsesColumnOne() {
        assertTarget(GoToLineLogic.parse("5", 10), 5, 1);
        assertTarget(GoToLineLogic.parse(" 5 ", 10), 5, 1);
        assertTarget(GoToLineLogic.parse("007", 10), 7, 1);
        assertTarget(GoToLineLogic.parse("10", 10), 10, 1);
        assertTarget(GoToLineLogic.parse("1", 10), 1, 1);
    }

    @Test
    void lineAndColumn() {
        assertTarget(GoToLineLogic.parse("5:3", 10), 5, 3);
        assertTarget(GoToLineLogic.parse("5 : 3", 10), 5, 3);
        assertTarget(GoToLineLogic.parse("5:123456789", 10), 5, 123456789);
    }

    @Test
    void nonNumbersAreFormatErrors() {
        assertError(GoToLineLogic.parse("abc", 10), FORMAT_ERROR);
        assertError(GoToLineLogic.parse("1.5", 10), FORMAT_ERROR);
        assertError(GoToLineLogic.parse("-1", 10), FORMAT_ERROR);
        assertError(GoToLineLogic.parse("\u0663", 10), FORMAT_ERROR);
    }

    @Test
    void missingPartsAreFormatErrors() {
        assertError(GoToLineLogic.parse("5:", 10), FORMAT_ERROR);
        assertError(GoToLineLogic.parse(":3", 10), FORMAT_ERROR);
        assertError(GoToLineLogic.parse("5:x", 10), FORMAT_ERROR);
        assertError(GoToLineLogic.parse("5:3:2", 10), FORMAT_ERROR);
    }

    @Test
    void tooManyDigitsIsAFormatError() {
        assertError(GoToLineLogic.parse("1234567890", 10), FORMAT_ERROR);
        assertError(GoToLineLogic.parse("5:1234567890", 10), FORMAT_ERROR);
    }

    @Test
    void lineOutsideTheDocument() {
        assertError(GoToLineLogic.parse("0", 10), "Line must be between 1 and 10");
        assertError(GoToLineLogic.parse("11", 10), "Line must be between 1 and 10");
        assertError(GoToLineLogic.parse("123456789", 10), "Line must be between 1 and 10");
        assertError(GoToLineLogic.parse("0:3", 10), "Line must be between 1 and 10");
    }

    @Test
    void columnZeroIsRejected() {
        assertError(GoToLineLogic.parse("5:0", 10), "Column must be 1 or more");
    }

    @Test
    void lineErrorComesBeforeColumnError() {
        assertError(GoToLineLogic.parse("11:0", 10), "Line must be between 1 and 10");
    }

    @Test
    void offsetAddsTheColumnToTheLineStart() {
        assertEquals(10, GoToLineLogic.offset(10, 5, 1));
        assertEquals(12, GoToLineLogic.offset(10, 5, 3));
        assertEquals(15, GoToLineLogic.offset(10, 5, 6));
    }

    @Test
    void offsetClampsToTheLine() {
        assertEquals(15, GoToLineLogic.offset(10, 5, 99));
        assertEquals(10, GoToLineLogic.offset(10, 5, 0));
        assertEquals(10, GoToLineLogic.offset(10, 5, -4));
        assertEquals(7, GoToLineLogic.offset(7, 0, 5));
        assertEquals(0, GoToLineLogic.offset(0, 0, 1));
    }
}