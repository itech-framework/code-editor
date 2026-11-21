package io.github.itech_framework.code_editor.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.itech_framework.code_editor.TextEdit;
import io.github.itech_framework.code_editor.formatting.FormatOptions;

class JsonFormatterTest {

    private static final FormatOptions TWO = new FormatOptions(2, true);

    private final JsonFormatter formatter = new JsonFormatter();

    private String format(String text, FormatOptions options) {
        List<TextEdit> edits = formatter.format(text, options);
        if (edits.isEmpty()) {
            return text;
        }
        TextEdit edit = edits.get(0);
        return text.substring(0, edit.start()) + edit.text() + text.substring(edit.end());
    }

    private String format(String text) {
        return format(text, TWO);
    }

    @Test
    void prettyPrintsWithKeyOrderAndNumberTextKept() {
        String input = "{\"a\":1.0E+10,\"b\":-0,\"c\":[],\"d\":{},\"e\":[1,[2,3]],\"f\":\"x y\",\"g\":{\"h\":null}}\n";
        String expected = String.join("\n",
                "{",
                "  \"a\": 1.0E+10,",
                "  \"b\": -0,",
                "  \"c\": [],",
                "  \"d\": {},",
                "  \"e\": [",
                "    1,",
                "    [",
                "      2,",
                "      3",
                "    ]",
                "  ],",
                "  \"f\": \"x y\",",
                "  \"g\": {",
                "    \"h\": null",
                "  }",
                "}") + "\n";
        assertEquals(expected, format(input));
    }

    @Test
    void usesTabsWhenSpacesAreOff() {
        assertEquals("[\n\t1,\n\t{\n\t\t\"a\": [\n\t\t\ttrue\n\t\t]\n\t}\n]",
                format("[1,{\"a\":[true]}]", new FormatOptions(4, false)));
    }

    @Test
    void usesTheTabSize() {
        assertEquals("[\n    1\n]", format("[1]", new FormatOptions(4, true)));
    }

    @Test
    void keepsKeyOrder() {
        assertEquals("{\n  \"b\": 1,\n  \"a\": 2\n}", format("{\"b\":1,\"a\":2}"));
    }

    @Test
    void keepsNumberTextExactly() {
        assertEquals("[\n  1.0E+10,\n  -0,\n  1e5\n]", format("[1.0E+10,-0,1e5]"));
    }

    @Test
    void doesNotTouchStringContent() {
        assertEquals("{\n  \"k\": \"a, b: {c} [d]\"\n}", format("{\"k\":\"a, b: {c} [d]\"}"));
    }

    @Test
    void keepsEscapesInStrings() {
        assertEquals("[\n  \"a\\\"b\",\n  \"\\\\\"\n]", format("[\"a\\\"b\",\"\\\\\"]"));
    }

    @Test
    void keepsEmptyContainersOnOneLine() {
        assertEquals("[\n  {},\n  []\n]", format("[ { } , [ ] ]"));
    }

    @Test
    void keepsALeadingBom() {
        assertEquals("\uFEFF[\n  1\n]", format("\uFEFF[1]"));
    }

    @Test
    void keepsAFinalNewlineOnlyWhenThereWasOne() {
        assertEquals("[\n  1\n]\n", format("[1]\n"));
        assertEquals("[\n  1\n]", format("[1]"));
    }

    @Test
    void formatsAScalarDocument() {
        assertEquals("42", format("  42  "));
    }

    @Test
    void messyWhitespaceIsNormalised() {
        assertEquals("{\n  \"a\": [\n    1,\n    2\n  ]\n}", format("{ \"a\" :\n[ 1 ,\t2 ]\r\n}"));
    }

    @Test
    void invalidJsonIsLeftAlone() {
        assertTrue(formatter.format("{\"a\":1,}", TWO).isEmpty());
        assertTrue(formatter.format("{\"a\":1}}", TWO).isEmpty());
        assertTrue(formatter.format("[1 2]", TWO).isEmpty());
        assertTrue(formatter.format("{\"a\":1,\"a\":2}", TWO).isEmpty());
    }

    @Test
    void blankTextIsLeftAlone() {
        assertTrue(formatter.format("", TWO).isEmpty());
        assertTrue(formatter.format("   \n", TWO).isEmpty());
    }

    @Test
    void alreadyFormattedTextProducesNoEdit() {
        assertTrue(formatter.format("{\n  \"a\": 1\n}", TWO).isEmpty());
        assertTrue(formatter.format("{\n  \"a\": 1\n}\n", TWO).isEmpty());
    }

    @Test
    void formattingTwiceChangesNothing() {
        String once = format("{\"a\":[1,{\"b\":null}],\"c\":\"d\"}");
        assertTrue(formatter.format(once, TWO).isEmpty());
    }

    @Test
    void producesASingleMinimalEdit() {
        List<TextEdit> edits = formatter.format("{\"a\":1}", TWO);
        assertEquals(List.of(new TextEdit(1, 6, "\n  \"a\": 1\n")), edits);
    }
}