package io.github.itech_framework.code_editor.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.github.itech_framework.code_editor.diagnostics.Diagnostic;
import io.github.itech_framework.code_editor.diagnostics.DiagnosticSeverity;
import io.github.itech_framework.code_editor.language.template.TemplateLanguages;

class LexicalDiagnosticsTest {

    private static final LexicalDiagnostics JS = new LexicalDiagnostics(
            TemplateLanguages.fromResource("javascript.json").getTokenizer());

    private static final LexicalDiagnostics JAVA = new LexicalDiagnostics(
            TemplateLanguages.fromResource("java.json").getTokenizer());

    private static String describe(List<Diagnostic> found) {
        return found.stream()
                .sorted(Comparator.comparingInt(Diagnostic::start))
                .map(d -> d.start() + ".." + d.end() + " " + d.message())
                .collect(Collectors.joining("; "));
    }

    private static String js(String text) {
        return describe(JS.diagnose(text));
    }

    private static String java(String text) {
        return describe(JAVA.diagnose(text));
    }

    @Test
    void tokenizerIsRequired() {
        assertThrows(NullPointerException.class, () -> new LexicalDiagnostics(null));
    }

    @Test
    void emptyTextHasNoDiagnostics() {
        assertEquals("", js(""));
    }

    @Test
    void balancedBracketsHaveNoDiagnostics() {
        assertEquals("", js("function f(a) { return [a, {b: (1)}]; }\n"));
    }

    @Test
    void allDiagnosticsAreErrors() {
        for (Diagnostic diagnostic : JS.diagnose("if (x) { foo(")) {
            assertEquals(DiagnosticSeverity.ERROR, diagnostic.severity());
        }
    }

    @Test
    void unclosedOpenersAreReported() {
        assertEquals("7..8 Unclosed '{'; 12..13 Unclosed '('", js("if (x) { foo("));
    }

    @Test
    void extraClosingBracketIsUnmatched() {
        assertEquals("5..6 Unmatched ')'", js("foo());\n"));
    }

    @Test
    void closerSkippingAnOpenerReportsTheOpener() {
        assertEquals("5..6 Unclosed '['", js("foo(a[1)\n"));
        assertEquals("2..3 Unclosed '('", js("{ ( }\n"));
    }

    @Test
    void crlfLinesKeepOffsetsRight() {
        assertEquals("1..2 Unclosed '('", js("a(\r\nb\r\n"));
    }

    @Test
    void bracketsInStringsAreIgnored() {
        assertEquals("", js("var s = \"(((\" + '}}' + `[[`;\n"));
    }

    @Test
    void bracketsInCommentsAreIgnored() {
        assertEquals("", js("// ((\n/* { [ */\nx();\n"));
    }

    @Test
    void bracketsInRegexAreIgnored() {
        assertEquals("", js("var r = /[(]+/g; var q = x.match(/\\)/);\n"));
    }

    @Test
    void unterminatedBlockCommentPointsAtItsStart() {
        assertEquals("5..13 Unterminated comment", js("a();\n/* hello\nworld\n"));
    }

    @Test
    void terminatedMultiLineCommentIsFine() {
        assertEquals("", js("a();\n/* hello\nworld */ b();\n"));
    }

    @Test
    void unterminatedDoubleQuotedString() {
        assertEquals("8..13 Unterminated string", js("var s = \"abc;\nvar t = 1;\n"));
    }

    @Test
    void unterminatedSingleQuotedString() {
        assertEquals("8..13 Unterminated string", js("var s = 'abc;\n"));
    }

    @Test
    void escapedClosingQuoteDoesNotCloseTheString() {
        assertEquals("8..15 Unterminated string", js("var s = \"abc\\\";\n"));
    }

    @Test
    void escapedBackslashBeforeTheQuoteStillCloses() {
        assertEquals("", js("var s = \"abc\\\\\";\n"));
    }

    @Test
    void emptyStringsAreClosed() {
        assertEquals("", js("var s = \"\", t = '';\n"));
    }

    @Test
    void unterminatedTemplateStringPointsAtItsStart() {
        assertEquals("8..12 Unterminated string", js("var s = `abc\ndef\n"));
    }

    @Test
    void multiLineTemplateStringIsFine() {
        assertEquals("", js("var s = `abc\n(def\n`;\n"));
    }
    
    @Test
    void commentOpenedAfterAnotherOneClosedIsTheUnterminatedOne() {
        assertEquals("16..20 Unterminated comment", js("/* a\n b */ x(); /* c\n"));
    }

    @Test
    void commentOpenedRightAfterAnotherOneClosed() {
        assertEquals("7..11 Unterminated comment", js("/* a\n*//* b\n"));
    }

    @Test
    void commentOpenedAndClosedAfterAnotherOneIsFine() {
        assertEquals("", js("/* a\n b */ x(); /* c */\n"));
    }

    @Test
    void javaTokensAreIgnoredInsideLiterals() {
        assertEquals("", java("public class A { void f() { char c = '('; String s = \"}\"; } }\n"));
    }

    @Test
    void javaTextBlockIsFine() {
        assertEquals("", java("String s = \"\"\"\n  \"hello (\n  world\"\"\";\n"));
    }

    @Test
    void javaTextBlockWithoutEnd() {
        assertEquals("11..14 Unterminated string", java("String s = \"\"\"\n  abc\n"));
    }

    @Test
    void javaUnterminatedCharLiteral() {
        assertEquals("9..12 Unterminated string", java("char c = 'a;\n"));
    }

    @Test
    void javaDocCommentWithoutEnd() {
        assertEquals("0..7 Unterminated comment", java("/** doc\n class A {}\n"));
    }

    @Test
    void javaImportDirectiveIsFine() {
        assertEquals("", java("import java.util.List;\nclass A {}\n"));
    }

    @Test
    void resultsAreCapped() {
        List<Diagnostic> found = JS.diagnose(")\n".repeat(LexicalDiagnostics.MAX_FOUND + 500));
        assertEquals(LexicalDiagnostics.MAX_FOUND, found.size());
    }

    @Test
    void tooLongTextIsSkipped() {
        assertTrue(JS.diagnose("(".repeat(LexicalDiagnostics.MAX_TEXT_LENGTH + 1)).isEmpty());
    }
}