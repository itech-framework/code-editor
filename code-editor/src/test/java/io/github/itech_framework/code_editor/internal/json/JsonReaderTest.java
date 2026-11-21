package io.github.itech_framework.code_editor.internal.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JsonReaderTest {

    private static JsonException fail(String text) {
        return assertThrows(JsonException.class, () -> JsonReader.parse(text));
    }

    @Test
    void readsScalars() {
        assertEquals(42L, JsonReader.parse("42"));
        assertEquals(-7L, JsonReader.parse("-7"));
        assertEquals(-0.5, JsonReader.parse("-0.5"));
        assertEquals(100.0, JsonReader.parse("1e2"));
        assertEquals(Boolean.TRUE, JsonReader.parse("true"));
        assertEquals(Boolean.FALSE, JsonReader.parse(" false "));
        assertNull(JsonReader.parse("null"));
    }

    @Test
    void largeIntegersFallBackToDouble() {
        assertTrue(JsonReader.parse("12345678901234567890") instanceof Double);
    }

    @Test
    void readsStringEscapes() {
        assertEquals("a\nb\t\"c\"\\/A", JsonReader.parse("\"a\\nb\\t\\\"c\\\"\\\\\\/\\u0041\""));
    }

    @Test
    void keepsKeyOrder() {
        Object value = JsonReader.parse("{\"b\":1,\"a\":2,\"c\":3}");
        assertTrue(value instanceof Map<?, ?>);
        assertEquals(List.of("b", "a", "c"), List.copyOf(((Map<?, ?>) value).keySet()));
    }

    @Test
    void readsNestedValues() {
        Object value = JsonReader.parse("{\"list\":[1,[2,{}],[]],\"empty\":{}}");
        Map<?, ?> map = (Map<?, ?>) value;
        assertEquals(List.of(1L, List.of(2L, Map.of()), List.of()), map.get("list"));
        assertEquals(Map.of(), map.get("empty"));
    }

    @Test
    void skipsLeadingBom() {
        assertEquals(1L, JsonReader.parse("\uFEFF1"));
    }

    @Test
    void acceptsDepthLimit() {
        JsonReader.parse("[".repeat(64) + "]".repeat(64));
    }

    @Test
    void rejectsTooDeepNesting() {
        JsonException ex = fail("[".repeat(65) + "]".repeat(65));
        assertEquals("Nesting deeper than 64 levels", ex.getReason());
        assertEquals(65, ex.getColumn());
    }

    @Test
    void reportsPositionOfErrors() {
        JsonException ex = fail("[1,\n 2,\n x]");
        assertEquals("Unexpected character 'x'", ex.getReason());
        assertEquals(3, ex.getLine());
        assertEquals(2, ex.getColumn());
        assertEquals("Unexpected character 'x' (line 3, column 2)", ex.getMessage());
    }

    @Test
    void rejectsTrailingCommas() {
        assertEquals("Expected a string key", fail("{\"a\":1,}").getReason());
        assertEquals("Unexpected character ']'", fail("[1,]").getReason());
    }

    @Test
    void rejectsDuplicateKeys() {
        JsonException ex = fail("{\"a\":1,\"a\":2}");
        assertEquals("Duplicate key 'a'", ex.getReason());
        assertEquals(8, ex.getColumn());
    }

    @Test
    void rejectsContentAfterValue() {
        JsonException ex = fail("{\"a\":1}\nx");
        assertEquals("Unexpected content after JSON value", ex.getReason());
        assertEquals(2, ex.getLine());
        assertEquals(1, ex.getColumn());
        assertEquals("Unexpected content after JSON value", fail("01").getReason());
    }

    @Test
    void rejectsBrokenStrings() {
        assertEquals("Unterminated string", fail("\"abc").getReason());
        assertEquals("Control character in string", fail("\"a\tb\"").getReason());
        assertEquals("Invalid escape '\\q'", fail("\"\\q\"").getReason());
        assertEquals("Invalid unicode escape", fail("\"\\u12\"").getReason());
    }

    @Test
    void rejectsBrokenNumbersAndLiterals() {
        assertEquals("Expected a digit", fail("-").getReason());
        assertEquals("Expected a digit", fail("1.").getReason());
        assertEquals("Invalid literal, expected true", fail("tru").getReason());
    }

    @Test
    void rejectsBrokenStructure() {
        assertEquals("Unexpected end of input", fail("").getReason());
        assertEquals("Expected ':'", fail("{\"a\" 1}").getReason());
        assertEquals("Expected a string key", fail("{1:2}").getReason());
        assertEquals("Expected ',' or ']' but found '2'", fail("[1 2]").getReason());
    }
}