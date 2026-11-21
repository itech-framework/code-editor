package io.github.itech_framework.code_editor.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.itech_framework.code_editor.diagnostics.Diagnostic;
import io.github.itech_framework.code_editor.diagnostics.DiagnosticSeverity;

class JsonDiagnosticsTest {

    private final JsonDiagnostics diagnostics = new JsonDiagnostics();

    private Diagnostic single(String text) {
        List<Diagnostic> found = diagnostics.diagnose(text);
        assertEquals(1, found.size());
        return found.get(0);
    }

    private void assertAt(String text, int start, int end, String message) {
        Diagnostic diagnostic = single(text);
        assertEquals(start, diagnostic.start());
        assertEquals(end, diagnostic.end());
        assertEquals(DiagnosticSeverity.ERROR, diagnostic.severity());
        assertEquals(message, diagnostic.message());
    }

    @Test
    void validJsonHasNoDiagnostics() {
        assertTrue(diagnostics.diagnose("{\"a\": [1, 2.5e3, null, true]}").isEmpty());
        assertTrue(diagnostics.diagnose("[]").isEmpty());
        assertTrue(diagnostics.diagnose("\"text\"").isEmpty());
    }

    @Test
    void blankTextHasNoDiagnostics() {
        assertTrue(diagnostics.diagnose("").isEmpty());
        assertTrue(diagnostics.diagnose("   \n ").isEmpty());
    }

    @Test
    void trailingCommaPointsAtTheClosingBracket() {
        assertAt("{\"a\": 1, \"b\": [1, 2,]}", 20, 21, "Unexpected character ']'");
    }

    @Test
    void unexpectedCharacterExtendsToTheEndOfTheWord() {
        assertAt("// hi\n{}", 0, 2, "Unexpected character '/'");
    }

    @Test
    void errorOnALaterLineHasTheRightOffset() {
        assertAt("{\n  \"a\": 1,\n  \"b\": x1\n}", 19, 21, "Unexpected character 'x'");
    }

    @Test
    void crlfLineEndingsKeepOffsetsRight() {
        assertAt("{\r\n\"a\": 1,\r\n\"b\": ?\r\n}", 17, 18, "Unexpected character '?'");
    }

    @Test
    void byteOrderMarkShiftsTheFirstLine() {
        assertAt("\uFEFF{\"a\": x}", 7, 8, "Unexpected character 'x'");
    }

    @Test
    void nonAsciiTextKeepsOffsetsRight() {
        assertAt("{\"\u1019\": 1, \"b\": q}", 14, 15, "Unexpected character 'q'");
    }

    @Test
    void surrogatePairIsNotSplit() {
        Diagnostic diagnostic = single("{\"a\": \uD83D\uDE00}");
        assertEquals(6, diagnostic.start());
        assertEquals(8, diagnostic.end());
    }

    @Test
    void duplicateKeyUnderlinesTheKey() {
        assertAt("{\"a\": 1, \"a\": 2}", 9, 12, "Duplicate key 'a'");
    }

    @Test
    void contentAfterTheValueIsReported() {
        assertAt("{} extra", 3, 8, "Unexpected content after JSON value");
    }

    @Test
    void missingCommaInObject() {
        assertAt("{\"a\": 1 \"b\": 2}", 8, 11, "Expected ',' or '}' but found '\"'");
    }

    @Test
    void missingCommaInArray() {
        assertAt("[1 2]", 3, 4, "Expected ',' or ']' but found '2'");
    }

    @Test
    void endOfInputGivesAnEmptyRangeAtTheEnd() {
        assertAt("[1,", 3, 3, "Unexpected end of input");
        assertAt("[1", 2, 2, "Unexpected end of input in array");
        assertAt("{\"a\": 1", 7, 7, "Unexpected end of input in object");
        assertAt("{\"a\": 1,", 8, 8, "Expected a string key");
        assertAt("\"abc", 4, 4, "Unterminated string");
    }

    @Test
    void offsetOfCountsLinesAndColumns() {
        assertEquals(0, JsonDiagnostics.offsetOf("ab\ncd", 1, 1));
        assertEquals(3, JsonDiagnostics.offsetOf("ab\ncd", 2, 1));
        assertEquals(4, JsonDiagnostics.offsetOf("ab\ncd", 2, 2));
    }

    @Test
    void offsetOfClampsToTheText() {
        assertEquals(2, JsonDiagnostics.offsetOf("ab", 3, 1));
        assertEquals(2, JsonDiagnostics.offsetOf("ab", 1, 10));
        assertEquals(0, JsonDiagnostics.offsetOf("ab", 1, 0));
    }

    @Test
    void offsetOfSkipsTheByteOrderMarkOnlyOnTheFirstLine() {
        assertEquals(1, JsonDiagnostics.offsetOf("\uFEFFab", 1, 1));
        assertEquals(3, JsonDiagnostics.offsetOf("\uFEFFa\nb", 2, 1));
    }

    @Test
    void extendStopsAtSeparators() {
        assertEquals(3, JsonDiagnostics.extend("abc def", 0));
        assertEquals(7, JsonDiagnostics.extend("abc def", 4));
        assertEquals(2, JsonDiagnostics.extend("a]b", 1));
        assertEquals(2, JsonDiagnostics.extend("a,b", 1));
    }

    @Test
    void extendClampsToTheText() {
        assertEquals(2, JsonDiagnostics.extend("ab", 2));
        assertEquals(2, JsonDiagnostics.extend("ab", 9));
    }

    @Test
    void extendKeepsSurrogatePairsTogether() {
        assertEquals(3, JsonDiagnostics.extend("x\uD83D\uDE00 y", 1));
    }
}