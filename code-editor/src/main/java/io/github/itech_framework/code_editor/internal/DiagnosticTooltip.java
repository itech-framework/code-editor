package io.github.itech_framework.code_editor.internal;

import java.util.Locale;

import io.github.itech_framework.code_editor.diagnostics.Diagnostic;
import javafx.scene.control.Label;

public final class DiagnosticTooltip extends Label {

    private static final double MAX_WIDTH = 420;

    private static final double MARGIN = 4;

    private static final String[] VARIANTS = { "diag-tooltip-error", "diag-tooltip-warning", "diag-tooltip-info" };

    public DiagnosticTooltip() {
        getStyleClass().add("diag-tooltip");
        setWrapText(true);
        setMaxWidth(MAX_WIDTH);
        setMouseTransparent(true);
        setFocusTraversable(false);
        setVisible(false);
        managedProperty().bind(visibleProperty());
    }

    public void show(Diagnostic diagnostic, double x, double y, double boundsWidth, double boundsHeight) {
        getStyleClass().removeAll(VARIANTS);
        getStyleClass().add("diag-tooltip-" + diagnostic.severity().name().toLowerCase(Locale.ROOT));
        setText(diagnostic.message());
        setTranslateX(0);
        setTranslateY(0);
        setVisible(true);
        applyCss();
        double width = Math.min(prefWidth(-1), MAX_WIDTH);
        double height = prefHeight(width);
        double left = x + 12;
        double top = y + 20;
        if (left + width > boundsWidth - MARGIN) {
            left = Math.max(MARGIN, boundsWidth - width - MARGIN);
        }
        if (top + height > boundsHeight - MARGIN) {
            top = Math.max(MARGIN, y - height - 8);
        }
        setTranslateX(left);
        setTranslateY(top);
    }

    public void dismiss() {
        setVisible(false);
    }
}