package io.github.itech_framework.code_editor.internal;

import java.util.List;

import io.github.itech_framework.code_editor.diagnostics.Diagnostic;
import io.github.itech_framework.code_editor.diagnostics.DiagnosticSeverity;
import io.github.itech_framework.code_editor.diagnostics.DiagnosticsProvider;
import io.github.itech_framework.code_editor.internal.json.JsonException;
import io.github.itech_framework.code_editor.internal.json.JsonReader;

public final class JsonDiagnostics implements DiagnosticsProvider {

    @Override
    public List<Diagnostic> diagnose(String text) {
        if (text.isBlank()) {
            return List.of();
        }
        try {
            JsonReader.parse(text);
            return List.of();
        } catch (JsonException ex) {
            int start = offsetOf(text, ex.getLine(), ex.getColumn());
            int end = extend(text, start);
            return List.of(new Diagnostic(start, end, DiagnosticSeverity.ERROR, ex.getReason()));
        }
    }

    static int offsetOf(String text, int line, int column) {
        int lineStart = 0;
        for (int current = 1; current < line; current++) {
            int next = text.indexOf('\n', lineStart);
            if (next < 0) {
                return text.length();
            }
            lineStart = next + 1;
        }
        int bom = line == 1 && !text.isEmpty() && text.charAt(0) == '\uFEFF' ? 1 : 0;
        return Math.min(text.length(), lineStart + Math.max(0, column - 1) + bom);
    }

    static int extend(String text, int offset) {
        int length = text.length();
        if (offset >= length) {
            return length;
        }
        int end = offset;
        while (end < length && !isBreak(text.charAt(end))) {
            end++;
        }
        if (end > offset) {
            return end;
        }
        return Character.isHighSurrogate(text.charAt(offset)) && offset + 1 < length ? offset + 2 : offset + 1;
    }

    private static boolean isBreak(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == ',' || c == ':' || c == '[' || c == ']'
                || c == '{' || c == '}';
    }
}