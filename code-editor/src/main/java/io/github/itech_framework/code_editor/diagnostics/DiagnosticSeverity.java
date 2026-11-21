package io.github.itech_framework.code_editor.diagnostics;

public enum DiagnosticSeverity {

    ERROR("diag-error"),
    WARNING("diag-warning"),
    INFO("diag-info");

    private final String styleClass;

    DiagnosticSeverity(String styleClass) {
        this.styleClass = styleClass;
    }

    public String styleClass() {
        return styleClass;
    }
}