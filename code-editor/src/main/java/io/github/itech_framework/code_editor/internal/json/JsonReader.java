package io.github.itech_framework.code_editor.internal.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class JsonReader {

    private static final int MAX_DEPTH = 64;

    private final String text;
    private int pos;
    private int line = 1;
    private int column = 1;

    private JsonReader(String text) {
        this.text = text;
    }

    public static Object parse(String text) {
        Objects.requireNonNull(text, "text");
        JsonReader reader = new JsonReader(text);
        if (reader.pos < text.length() && text.charAt(0) == '\uFEFF') {
            reader.pos = 1;
        }
        reader.skipWhitespace();
        Object value = reader.readValue(0);
        reader.skipWhitespace();
        if (!reader.atEnd()) {
            throw reader.error("Unexpected content after JSON value");
        }
        return value;
    }

    private Object readValue(int depth) {
        if (atEnd()) {
            throw error("Unexpected end of input");
        }
        char c = peek();
        switch (c) {
            case '{':
                return readObject(depth);
            case '[':
                return readArray(depth);
            case '"':
                return readString();
            case 't':
                readLiteral("true");
                return Boolean.TRUE;
            case 'f':
                readLiteral("false");
                return Boolean.FALSE;
            case 'n':
                readLiteral("null");
                return null;
            default:
                if (c == '-' || isDigit(c)) {
                    return readNumber();
                }
                throw error("Unexpected character '" + c + "'");
        }
    }

    private Map<String, Object> readObject(int depth) {
        if (depth >= MAX_DEPTH) {
            throw error("Nesting deeper than " + MAX_DEPTH + " levels");
        }
        next();
        Map<String, Object> map = new LinkedHashMap<>();
        skipWhitespace();
        if (!atEnd() && peek() == '}') {
            next();
            return map;
        }
        while (true) {
            skipWhitespace();
            if (atEnd() || peek() != '"') {
                throw error("Expected a string key");
            }
            int keyLine = line;
            int keyColumn = column;
            String key = readString();
            if (map.containsKey(key)) {
                throw new JsonException("Duplicate key '" + key + "'", keyLine, keyColumn);
            }
            skipWhitespace();
            expect(':');
            skipWhitespace();
            map.put(key, readValue(depth + 1));
            skipWhitespace();
            if (atEnd()) {
                throw error("Unexpected end of input in object");
            }
            char c = peek();
            if (c == ',') {
                next();
                continue;
            }
            if (c == '}') {
                next();
                return map;
            }
            throw error("Expected ',' or '}' but found '" + c + "'");
        }
    }

    private List<Object> readArray(int depth) {
        if (depth >= MAX_DEPTH) {
            throw error("Nesting deeper than " + MAX_DEPTH + " levels");
        }
        next();
        List<Object> list = new ArrayList<>();
        skipWhitespace();
        if (!atEnd() && peek() == ']') {
            next();
            return list;
        }
        while (true) {
            skipWhitespace();
            list.add(readValue(depth + 1));
            skipWhitespace();
            if (atEnd()) {
                throw error("Unexpected end of input in array");
            }
            char c = peek();
            if (c == ',') {
                next();
                continue;
            }
            if (c == ']') {
                next();
                return list;
            }
            throw error("Expected ',' or ']' but found '" + c + "'");
        }
    }

    private String readString() {
        next();
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (atEnd()) {
                throw error("Unterminated string");
            }
            char c = next();
            if (c == '"') {
                return sb.toString();
            }
            if (c < 0x20) {
                throw error("Control character in string");
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (atEnd()) {
                throw error("Unterminated string");
            }
            char e = next();
            switch (e) {
                case '"':
                    sb.append('"');
                    break;
                case '\\':
                    sb.append('\\');
                    break;
                case '/':
                    sb.append('/');
                    break;
                case 'b':
                    sb.append('\b');
                    break;
                case 'f':
                    sb.append('\f');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'u':
                    sb.append(readUnicode());
                    break;
                default:
                    throw error("Invalid escape '\\" + e + "'");
            }
        }
    }

    private char readUnicode() {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            if (atEnd()) {
                throw error("Incomplete unicode escape");
            }
            int digit = Character.digit(next(), 16);
            if (digit < 0) {
                throw error("Invalid unicode escape");
            }
            value = value * 16 + digit;
        }
        return (char) value;
    }

    private Number readNumber() {
        int start = pos;
        if (peek() == '-') {
            next();
        }
        if (!atEnd() && peek() == '0') {
            next();
        } else {
            requireDigits();
        }
        boolean integer = true;
        if (!atEnd() && peek() == '.') {
            integer = false;
            next();
            requireDigits();
        }
        if (!atEnd() && (peek() == 'e' || peek() == 'E')) {
            integer = false;
            next();
            if (!atEnd() && (peek() == '+' || peek() == '-')) {
                next();
            }
            requireDigits();
        }
        String literal = text.substring(start, pos);
        if (integer) {
            try {
                return Long.parseLong(literal);
            } catch (NumberFormatException ex) {
                return Double.parseDouble(literal);
            }
        }
        return Double.parseDouble(literal);
    }

    private void requireDigits() {
        if (atEnd() || !isDigit(peek())) {
            throw error("Expected a digit");
        }
        while (!atEnd() && isDigit(peek())) {
            next();
        }
    }

    private void readLiteral(String word) {
        for (int i = 0; i < word.length(); i++) {
            if (atEnd() || peek() != word.charAt(i)) {
                throw error("Invalid literal, expected " + word);
            }
            next();
        }
    }

    private void expect(char expected) {
        if (atEnd() || peek() != expected) {
            throw error("Expected '" + expected + "'");
        }
        next();
    }

    private void skipWhitespace() {
        while (!atEnd()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                next();
            } else {
                return;
            }
        }
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean atEnd() {
        return pos >= text.length();
    }

    private char peek() {
        return text.charAt(pos);
    }

    private char next() {
        char c = text.charAt(pos++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private JsonException error(String message) {
        return new JsonException(message, line, column);
    }
}