package io.github.itech_framework.code_editor.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.itech_framework.code_editor.TextEdit;
import io.github.itech_framework.code_editor.formatting.CodeFormatter;
import io.github.itech_framework.code_editor.formatting.FormatOptions;
import io.github.itech_framework.code_editor.language.EditorLanguage;
import io.github.itech_framework.code_editor.language.FormatterSpec;
import io.github.itech_framework.code_editor.language.template.TemplateLanguages;
import io.github.itech_framework.code_editor.language.template.TemplateTokenizer;

class BraceFormatterTest {

    private static final FormatOptions TWO = new FormatOptions(2, true);

    private static final EditorLanguage JAVASCRIPT = TemplateLanguages.fromResource("javascript.json");

    private static final EditorLanguage JAVA = TemplateLanguages.fromResource("java.json");

    private final CodeFormatter javascript = BuiltInFormatters.forLanguage(JAVASCRIPT);

    private static String apply(CodeFormatter formatter, String text, FormatOptions options) {
        List<TextEdit> edits = formatter.format(text, options);
        if (edits.isEmpty()) {
            return text;
        }
        TextEdit edit = edits.get(0);
        return text.substring(0, edit.start()) + edit.text() + text.substring(edit.end());
    }

    private String format(String text) {
        return apply(javascript, text, TWO);
    }

    private static CodeFormatter custom(boolean trim, boolean finalNewline, List<io.github.itech_framework.code_editor.language.BracketPair> brackets) {
        return new BraceFormatter(JAVASCRIPT.getTokenizer(),
                new FormatterSpec(FormatterSpec.Type.BRACE, brackets, trim, finalNewline));
    }

    @Test
    void indentsABlock() {
        assertEquals("function f(a){\n  return 1;\n}\n", format("function f(a){\nreturn 1;\n}\n"));
    }

    @Test
    void dedentsClosingBracketAndKeepsElseOnTheSameLine() {
        assertEquals("if (a) {\n  b();\n} else {\n  c();\n}", format("if (a) {\nb();\n} else {\nc();\n}"));
    }

    @Test
    void hangingCallbackIndentsOnlyOneLevel() {
        assertEquals("foo(function() {\n  bar();\n});", format("foo(function() {\nbar();\n});"));
    }

    @Test
    void indentsMultiLineArrays() {
        assertEquals("const a = [\n  1,\n  2\n];", format("const a = [\n1,\n2\n];"));
    }

    @Test
    void reindentsFlushAndOverIndentedLines() {
        assertEquals("{\n  a;\n  b;\n}", format("{\n      a;\n b;\n        }"));
    }

    @Test
    void usesTabsWhenSpacesAreOff() {
        assertEquals("{\n\ta;\n}", apply(javascript, "{\na;\n}", new FormatOptions(4, false)));
    }

    @Test
    void usesTheTabSize() {
        assertEquals("{\n    a;\n}", apply(javascript, "{\na;\n}", new FormatOptions(4, true)));
    }

    @Test
    void ignoresBracketsInsideStrings() {
        assertEquals("x = \"{\";\ny = 1;", format("x = \"{\";\n   y = 1;"));
    }

    @Test
    void ignoresBracketsInsideLineComments() {
        assertEquals("a();\n// {\nb();", format("  a();\n    // {\n  b();"));
    }

    @Test
    void leavesBlockCommentInteriorUntouched() {
        assertEquals("/*\n   * keep {\n */\nx();", format("/*\n   * keep {\n */\n   x();  "));
    }

    @Test
    void leavesMultiLineTemplateLiteralUntouched() {
        assertEquals("const t = `a\n   {  \n  b`;\nx();", format("const t = `a\n   {  \n  b`;\n  x();"));
    }

    @Test
    void trimsTrailingWhitespaceAndEmptiesBlankLines() {
        assertEquals("a();\n\nb();", format("a();   \n   \nb();\t"));
    }

    @Test
    void keepsCrLfLineEndings() {
        assertEquals("if (a) {\r\n  b();\r\n}\r\n", format("if (a) {\r\nb();\r\n}\r\n"));
    }

    @Test
    void unmatchedClosingBracketDoesNotGoNegative() {
        assertEquals("}\nx();", format("  }\n  x();"));
    }

    @Test
    void indentsCaseBodies() {
        assertEquals("switch (x) {\n  case 1:\n    foo();\n    break;\n  default:\n    bar();\n}",
                format("switch (x) {\ncase 1:\nfoo();\nbreak;\ndefault:\nbar();\n}"));
    }

    @Test
    void indentsALineAfterATrailingOperator() {
        assertEquals("x = a +\n  b;", format("x = a +\nb;"));
        assertEquals("f = () =>\n  x;", format("f = () =>\nx;"));
    }

    @Test
    void indentsMethodChains() {
        assertEquals("a\n  .b()\n  .c();", format("a\n.b()\n.c();"));
    }

    @Test
    void incrementAtTheEndOfALineIsNotAContinuation() {
        assertEquals("x++\ny", format("x++\n  y"));
    }

    @Test
    void formattingTwiceChangesNothing() {
        String once = format("function f(a){\nif (a) {\nfoo(function() {\nbar();\n});\n} else {\nx = a +\nb;\n}\nswitch (a) {\ncase 1:\nz();\n}\n}\n");
        assertTrue(javascript.format(once, TWO).isEmpty());
    }

    @Test
    void javaTextBlockContentIsLeftAlone() {
        CodeFormatter java = BuiltInFormatters.forLanguage(JAVA);
        String input = "class A {\nvoid m() {\nString s = \"\"\"\n   keep {\n   \"\"\";\nif (x) {\n}\n}\n}";
        String expected = "class A {\n  void m() {\n    String s = \"\"\"\n   keep {\n   \"\"\";\n    if (x) {\n    }\n  }\n}";
        assertEquals(expected, apply(java, input, TWO));
    }

    @Test
    void finalNewlineIsAddedWhenRequested() {
        CodeFormatter formatter = custom(true, true, FormatterSpec.DEFAULT_BRACKETS);
        assertEquals("a();\n", apply(formatter, "a();", TWO));
        assertTrue(formatter.format("a();\n", TWO).isEmpty());
    }

    @Test
    void trailingWhitespaceIsKeptWhenTrimIsOff() {
        CodeFormatter formatter = custom(false, false, FormatterSpec.DEFAULT_BRACKETS);
        assertEquals("a();  \n   \nb();", apply(formatter, "a();  \n   \n  b();", TWO));
    }

    @Test
    void onlyTheConfiguredBracketsIndent() {
        CodeFormatter formatter = custom(true, false, List.of(new io.github.itech_framework.code_editor.language.BracketPair('{', '}')));
        assertEquals("foo(\nx\n)", apply(formatter, "foo(\n  x\n  )", TWO));
        assertEquals("{\n  x\n}", apply(formatter, "{\nx\n}", TWO));
    }

    @Test
    void blankTextIsLeftAlone() {
        assertTrue(javascript.format("", TWO).isEmpty());
        assertTrue(javascript.format("  \n ", TWO).isEmpty());
    }

    @Test
    void oversizedTextIsLeftAlone() {
        assertTrue(javascript.format("x".repeat(BraceFormatter.MAX_TEXT_LENGTH + 1), TWO).isEmpty());
    }

    @Test
    void overlongLinesAreLeftAlone() {
        assertTrue(javascript.format("{\n" + "x".repeat(TemplateTokenizer.MAX_LINE_LENGTH + 1) + "\n}", TWO).isEmpty());
    }

    @Test
    void alreadyFormattedTextProducesNoEdit() {
        assertTrue(javascript.format("function f() {\n  return 1;\n}\n", TWO).isEmpty());
    }
}