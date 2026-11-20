package io.github.itech_framework.code_editor;

import java.util.Objects;

public record TextEdit(int start, int end, String text) {

    public TextEdit {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Invalid edit range: " + start + ".." + end);
        }
        Objects.requireNonNull(text, "text");
    }
}