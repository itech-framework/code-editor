package io.github.itech_framework.code_editor.diagnostics;

import java.util.List;

@FunctionalInterface
public interface DiagnosticsProvider {

    List<Diagnostic> diagnose(String text);
}