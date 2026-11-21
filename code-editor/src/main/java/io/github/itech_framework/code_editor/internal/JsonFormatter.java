package io.github.itech_framework.code_editor.internal;

import java.util.List;

import io.github.itech_framework.code_editor.TextEdit;
import io.github.itech_framework.code_editor.formatting.CodeFormatter;
import io.github.itech_framework.code_editor.formatting.FormatOptions;
import io.github.itech_framework.code_editor.internal.json.JsonException;
import io.github.itech_framework.code_editor.internal.json.JsonReader;

public final class JsonFormatter implements CodeFormatter {

    @Override
    public List<TextEdit> format(String text, FormatOptions options) {
        if (text.isBlank()) {
            return List.of();
        }
        try {
            JsonReader.parse(text);
        } catch (JsonException ex) {
            return List.of();
        }
        TextEdit edit = FormatLogic.diff(text, render(text, options.indentUnit()));
        return edit == null ? List.of() : List.of(edit);
    }

    static String render(String text, String unit) {
        int length = text.length();
        StringBuilder out = new StringBuilder(length + length / 4);
        int i = 0;
        if (text.charAt(0) == '\uFEFF') {
            out.append('\uFEFF');
            i = 1;
        }
        int depth = 0;
        while (i < length) {
            char c = text.charAt(i);
            if (isWhitespace(c)) {
                i++;
            } else if (c == '{' || c == '[') {
                int next = skipWhitespace(text, i + 1);
                char close = c == '{' ? '}' : ']';
                if (next < length && text.charAt(next) == close) {
                    out.append(c).append(close);
                    i = next + 1;
                } else {
                    out.append(c);
                    depth++;
                    newline(out, unit, depth);
                    i++;
                }
            } else if (c == '}' || c == ']') {
                depth--;
                newline(out, unit, depth);
                out.append(c);
                i++;
            } else if (c == ',') {
                out.append(',');
                newline(out, unit, depth);
                i++;
            } else if (c == ':') {
                out.append(": ");
                i++;
            } else if (c == '"') {
                int end = stringEnd(text, i);
                out.append(text, i, end);
                i = end;
            } else {
                int end = i;
                while (end < length && !isDelimiter(text.charAt(end))) {
                    end++;
                }
                out.append(text, i, end);
                i = end;
            }
        }
        if (text.endsWith("\n")) {
            out.append('\n');
        }
        return out.toString();
    }

    private static void newline(StringBuilder out, String unit, int depth) {
        out.append('\n');
        for (int i = 0; i < depth; i++) {
            out.append(unit);
        }
    }

    private static int skipWhitespace(String text, int from) {
        int i = from;
        while (i < text.length() && isWhitespace(text.charAt(i))) {
            i++;
        }
        return i;
    }

    private static int stringEnd(String text, int start) {
        int i = start + 1;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == '"') {
                return i + 1;
            } else {
                i++;
            }
        }
        return text.length();
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private static boolean isDelimiter(char c) {
        return isWhitespace(c) || c == ',' || c == ':' || c == '{' || c == '}' || c == '[' || c == ']' || c == '"';
    }
}