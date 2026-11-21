package io.github.itech_framework.code_editor.language.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.itech_framework.code_editor.language.BracketPair;
import io.github.itech_framework.code_editor.language.FormatterSpec;

class TemplateLoaderTest {

    private static final String MINIMAL = "{\"version\":1,\"id\":\"x\",\"states\":{\"root\":[]}}";

    private static String error(String json) {
        return assertThrows(TemplateException.class, () -> TemplateLoader.load("t", json)).getMessage();
    }

    private static String withRule(String rule) {
        return "{\"version\":1,\"id\":\"x\",\"states\":{\"root\":[" + rule + "]}}";
    }

    private static String withField(String field) {
        return "{\"version\":1,\"id\":\"x\"," + field + ",\"states\":{\"root\":[]}}";
    }

    @Test
    void minimalTemplateGetsDefaults() {
        LanguageTemplate template = TemplateLoader.load("t", MINIMAL);
        assertEquals("x", template.id());
        assertEquals("x", template.displayName());
        assertEquals("root", template.startState());
        assertEquals(List.of(), template.extensions());
        assertEquals(BracketPair.DEFAULT_AUTO_CLOSING, template.autoClosingPairs());
        assertNull(template.lineComment());
        assertNull(template.formatter());
    }

    @Test
    void readsOptionalFields() {
        LanguageTemplate template = TemplateLoader.load("t",
                "{\"version\":1,\"id\":\"x\",\"name\":\"X\",\"extensions\":[\" js \"],\"lineComment\":\" // \","
                        + "\"start\":\"b\",\"states\":{\"root\":[],\"b\":[]}}");
        assertEquals("X", template.displayName());
        assertEquals(List.of("js"), template.extensions());
        assertEquals("//", template.lineComment());
        assertEquals("b", template.startState());
        assertEquals(List.of("root", "b"), List.copyOf(template.states().keySet()));
    }

    @Test
    void readsFormatterSettings() {
        assertEquals(FormatterSpec.Type.JSON,
                TemplateLoader.load("t", withField("\"formatter\":{\"type\":\"json\"}")).formatter().type());
        FormatterSpec brace = TemplateLoader.load("t", withField("\"formatter\":{\"type\":\"brace\"}")).formatter();
        assertEquals(FormatterSpec.DEFAULT_BRACKETS, brace.brackets());
        assertTrue(brace.trimTrailingWhitespace());
        FormatterSpec custom = TemplateLoader.load("t", withField("\"formatter\":{\"type\":\"brace\","
                + "\"brackets\":[\"{}\",\"<>\"],\"trimTrailingWhitespace\":false,\"insertFinalNewline\":true}"))
                .formatter();
        assertEquals(List.of(new BracketPair('{', '}'), new BracketPair('<', '>')), custom.brackets());
        assertEquals(false, custom.trimTrailingWhitespace());
        assertTrue(custom.insertFinalNewline());
    }

    @Test
    void rejectsBadFormatterSettings() {
        assertEquals("t: formatter: must be an object", error(withField("\"formatter\":5")));
        assertEquals("t: formatter.type: is required", error(withField("\"formatter\":{}")));
        assertEquals("t: formatter.type: unknown formatter type 'nope'. Supported types: brace, json",
                error(withField("\"formatter\":{\"type\":\"nope\"}")));
        assertEquals("t: formatter.brackets: is only allowed with type 'brace'",
                error(withField("\"formatter\":{\"type\":\"json\",\"brackets\":[\"()\"]}")));
        assertEquals("t: formatter.bogus: unknown field",
                error(withField("\"formatter\":{\"type\":\"brace\",\"bogus\":1}")));
        assertEquals("t: formatter.brackets: must contain at least one pair",
                error(withField("\"formatter\":{\"type\":\"brace\",\"brackets\":[]}")));
        assertEquals("t: formatter.trimTrailingWhitespace: must be true or false",
                error(withField("\"formatter\":{\"type\":\"brace\",\"trimTrailingWhitespace\":1}")));
        assertEquals("t: formatter.brackets[0]: the opening and the closing character must differ",
                error(withField("\"formatter\":{\"type\":\"brace\",\"brackets\":[\"((\"]}")));
        assertEquals("t: formatter.brackets[0]: must be a string of exactly two characters: the opening and the closing bracket",
                error(withField("\"formatter\":{\"type\":\"brace\",\"brackets\":[\"(\"]}")));
        assertEquals("t: formatter.brackets[0]: characters must not be whitespace",
                error(withField("\"formatter\":{\"type\":\"brace\",\"brackets\":[\" )\"]}")));
        assertEquals("t: formatter.brackets[1]: duplicate opening character '('",
                error(withField("\"formatter\":{\"type\":\"brace\",\"brackets\":[\"()\",\"(]\"]}")));
        assertEquals("t: formatter.brackets[1]: duplicate closing character ')'",
                error(withField("\"formatter\":{\"type\":\"brace\",\"brackets\":[\"()\",\"[)\"]}")));
    }

    @Test
    void rejectsBadRoot() {
        assertEquals("t: (root): must be an object", error("[]"));
        assertEquals("t: zzz: unknown field", error(withField("\"zzz\":1")));
        assertTrue(error("{\"a\":").startsWith("t: json: Unexpected end of input"));
    }

    @Test
    void rejectsBadVersion() {
        assertEquals("t: version: is required", error("{}"));
        assertEquals("t: version: must be an integer", error("{\"version\":\"1\"}"));
        assertEquals("t: version: unsupported value 2, expected 1", error("{\"version\":2}"));
    }

    @Test
    void rejectsMissingParts() {
        assertEquals("t: id: is required", error("{\"version\":1,\"states\":{\"root\":[]}}"));
        assertEquals("t: states: is required", error("{\"version\":1,\"id\":\"x\"}"));
        assertEquals("t: states: must define at least one state", error("{\"version\":1,\"id\":\"x\",\"states\":{}}"));
        assertEquals("t: extensions[0]: must be a non-blank string", error(withField("\"extensions\":[\"\"]")));
    }

    @Test
    void rejectsUnknownStates() {
        assertEquals("t: start: unknown state 'main'. Defined states: [root]", error(withField("\"start\":\"main\"")));
        assertEquals("t: states.root[0].next: unknown state 'nope'. Defined states: [root]",
                error(withRule("{\"match\":\"a\",\"scope\":\"a\",\"next\":\"nope\"}")));
    }

    @Test
    void rejectsBadRules() {
        assertEquals("t: states.root[0].match: invalid regex: Unclosed group",
                error(withRule("{\"match\":\"(\",\"scope\":\"a\"}")));
        assertEquals("t: states.root[0].match: regex must not match an empty string",
                error(withRule("{\"match\":\"a*\",\"scope\":\"a\"}")));
        assertEquals("t: states.root[0]: needs exactly one of 'match', 'words', 'begin' or 'eol'",
                error(withRule("{\"scope\":\"a\"}")));
        assertEquals("t: states.root[0]: needs exactly one of 'match', 'words', 'begin' or 'eol'",
                error(withRule("{\"match\":\"a\",\"words\":[\"b\"],\"scope\":\"a\"}")));
        assertEquals("t: states.root[0].end: is required with 'begin'",
                error(withRule("{\"begin\":\"a\",\"scope\":\"a\"}")));
        assertEquals("t: states.root[0].words: must contain at least one word",
                error(withRule("{\"words\":[],\"scope\":\"a\"}")));
        assertEquals("t: states.root[0].words[0]: must not contain whitespace",
                error(withRule("{\"words\":[\"a b\"],\"scope\":\"a\"}")));
        assertEquals("t: states.root[0]: use only one of 'next', 'push' or 'pop'",
                error(withRule("{\"match\":\"a\",\"scope\":\"a\",\"next\":\"root\",\"pop\":true}")));
        assertEquals("t: states.root[0].scope: is not allowed in eol rules",
                error(withRule("{\"eol\":true,\"scope\":\"a\"}")));
        assertEquals("t: states.root[0]: 'eol' rules need one of 'next', 'push' or 'pop'",
                error(withRule("{\"eol\":true}")));
    }

    @Test
    void rejectsBadLineCommentAndPairs() {
        assertEquals("t: lineComment: must be a single line", error(withField("\"lineComment\":\"a\\nb\"")));
        assertEquals("t: autoClosingPairs[0]: must be an array of two strings: the opening and the closing character",
                error(withField("\"autoClosingPairs\":[[\"(\"]]")));
        assertEquals("t: autoClosingPairs[1]: duplicate opening character '('",
                error(withField("\"autoClosingPairs\":[[\"(\",\")\"],[\"(\",\"]\"]]")));
    }
}