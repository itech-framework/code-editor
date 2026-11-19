package io.github.itech_framework.code_editor.language;

public record Token(int start, int end, String scope) {

    public Token {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Invalid token range: " + start + ".." + end);
        }
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("Token scope must not be blank");
        }
    }

    public int length() {
        return end - start;
    }
}