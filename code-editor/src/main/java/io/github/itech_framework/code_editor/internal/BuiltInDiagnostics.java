package io.github.itech_framework.code_editor.internal;

import io.github.itech_framework.code_editor.diagnostics.DiagnosticsProvider;

public final class BuiltInDiagnostics {

    private static final DiagnosticsProvider JSON = new JsonDiagnostics();

    private BuiltInDiagnostics() {
    }

    public static DiagnosticsProvider forLanguage(String languageId) {
        return "json".equals(languageId) ? JSON : null;
    }
}