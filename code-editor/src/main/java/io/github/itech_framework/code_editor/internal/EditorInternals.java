package io.github.itech_framework.code_editor.internal;

import java.util.Objects;

import javafx.scene.Node;

public final class EditorInternals {

    private static final String KEY = EditorInternals.class.getName();

    private final FindPanel findPanel;
    private final GoToLinePanel goToLinePanel;
    private final DiagnosticTooltip diagnosticTooltip;
    private final CompletionPopup completionPopup;
    private final FoldState foldState;

    public EditorInternals(FindPanel findPanel, GoToLinePanel goToLinePanel, DiagnosticTooltip diagnosticTooltip,
            CompletionPopup completionPopup, FoldState foldState) {
        this.findPanel = Objects.requireNonNull(findPanel, "findPanel");
        this.goToLinePanel = Objects.requireNonNull(goToLinePanel, "goToLinePanel");
        this.diagnosticTooltip = Objects.requireNonNull(diagnosticTooltip, "diagnosticTooltip");
        this.completionPopup = Objects.requireNonNull(completionPopup, "completionPopup");
        this.foldState = Objects.requireNonNull(foldState, "foldState");
    }

    public static void attach(Node editor, EditorInternals internals) {
        editor.getProperties().put(KEY, internals);
    }

    public static EditorInternals of(Node editor) {
        if (editor.getProperties().get(KEY) instanceof EditorInternals internals) {
            return internals;
        }
        throw new IllegalStateException("Editor internals are not attached");
    }

    public FindPanel findPanel() {
        return findPanel;
    }

    public GoToLinePanel goToLinePanel() {
        return goToLinePanel;
    }

    public DiagnosticTooltip diagnosticTooltip() {
        return diagnosticTooltip;
    }

    public CompletionPopup completionPopup() {
        return completionPopup;
    }

    public FoldState foldState() {
        return foldState;
    }
}