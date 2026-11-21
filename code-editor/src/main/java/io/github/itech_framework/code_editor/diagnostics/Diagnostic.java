package io.github.itech_framework.code_editor.diagnostics;

import java.util.Objects;

public record Diagnostic(int start, int end, DiagnosticSeverity severity, String message) {

    public Diagnostic {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Invalid diagnostic range: " + start + ".." + end);
        }
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(message, "message");
    }
}