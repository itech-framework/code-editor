package io.github.itech_framework.code_editor.formatting;

public record FormatOptions(int tabSize, boolean insertSpaces) {

    public FormatOptions {
        if (tabSize < 1) {
            throw new IllegalArgumentException("tabSize must be at least 1");
        }
    }

    public String indentUnit() {
        return insertSpaces ? " ".repeat(tabSize) : "\t";
    }
}