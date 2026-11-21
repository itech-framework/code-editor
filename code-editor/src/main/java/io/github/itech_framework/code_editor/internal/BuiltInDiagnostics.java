package io.github.itech_framework.code_editor.internal;

import io.github.itech_framework.code_editor.diagnostics.DiagnosticsProvider;
import io.github.itech_framework.code_editor.language.EditorLanguage;
import io.github.itech_framework.code_editor.language.template.TemplateTokenizer;

public final class BuiltInDiagnostics {

    private static final DiagnosticsProvider JSON = new JsonDiagnostics();

    private BuiltInDiagnostics() {
    }

    public static DiagnosticsProvider forLanguage(EditorLanguage language) {
        if (language == null) {
            return null;
        }
        if ("json".equals(language.getId())) {
            return JSON;
        }
        if (language.getTokenizer() instanceof TemplateTokenizer) {
            return new LexicalDiagnostics(language.getTokenizer());
        }
        return null;
    }
}