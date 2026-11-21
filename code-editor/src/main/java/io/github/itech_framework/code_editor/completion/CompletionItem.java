package io.github.itech_framework.code_editor.completion;

import java.util.Objects;

public record CompletionItem(String label, String insertText, Kind kind, int start, int end) {

    public enum Kind {
        KEYWORD, WORD, VARIABLE
    }

    public CompletionItem {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(insertText, "insertText");
        Objects.requireNonNull(kind, "kind");
        if (label.isEmpty()) {
            throw new IllegalArgumentException("Completion label must not be empty");
        }
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Invalid replace range: " + start + ".." + end);
        }
    }

    public static CompletionItem of(String text, Kind kind, int start, int end) {
        return new CompletionItem(text, text, kind, start, end);
    }
}