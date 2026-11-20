package io.github.itech_framework.code_editor.language;

import java.util.List;

public record BracketPair(char open, char close) {

    public static final List<BracketPair> DEFAULT_AUTO_CLOSING = List.of(
            new BracketPair('(', ')'),
            new BracketPair('[', ']'),
            new BracketPair('{', '}'),
            new BracketPair('"', '"'),
            new BracketPair('\'', '\''));

    public BracketPair {
        if (Character.isWhitespace(open) || Character.isWhitespace(close)) {
            throw new IllegalArgumentException("Bracket characters must not be whitespace");
        }
    }

    public boolean isSymmetric() {
        return open == close;
    }
}