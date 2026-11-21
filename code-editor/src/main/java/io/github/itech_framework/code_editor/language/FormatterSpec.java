package io.github.itech_framework.code_editor.language;

import java.util.List;
import java.util.Objects;

public record FormatterSpec(Type type, List<BracketPair> brackets, boolean trimTrailingWhitespace,
        boolean insertFinalNewline) {

    public enum Type {
        JSON, BRACE
    }

    public static final List<BracketPair> DEFAULT_BRACKETS = List.of(
            new BracketPair('{', '}'),
            new BracketPair('[', ']'),
            new BracketPair('(', ')'));

    public FormatterSpec {
        Objects.requireNonNull(type, "type");
        brackets = List.copyOf(brackets);
    }

    public static FormatterSpec json() {
        return new FormatterSpec(Type.JSON, DEFAULT_BRACKETS, true, false);
    }
}