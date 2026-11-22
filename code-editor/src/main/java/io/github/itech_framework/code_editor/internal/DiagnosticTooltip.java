package io.github.itech_framework.code_editor.internal;

import java.util.Locale;

import io.github.itech_framework.code_editor.diagnostics.Diagnostic;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Control;
import javafx.scene.control.Label;

public final class DiagnosticTooltip extends Label {

    private static final double MAX_WIDTH = 420;

    private static final double MARGIN = 4;

    private static final String[] VARIANTS = { "diag-tooltip-error", "diag-tooltip-warning", "diag-tooltip-info" };

    private final FloatingHost host = new FloatingHost(this, this::dismiss);

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
        style(diagnostic);
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

    public void showInWindow(Control owner, Diagnostic diagnostic, double screenX, double screenY) {
        style(diagnostic);
        setTranslateX(0);
        setTranslateY(0);
        setVisible(true);
        host.show(owner, screenX + 12, screenY + 20);
        applyCss();
        double width = Math.min(prefWidth(-1), MAX_WIDTH);
        double height = prefHeight(width);
        Rectangle2D screen = FloatingHost.screenBounds(screenX, screenY);
        double left = Math.max(screen.getMinX() + MARGIN,
                Math.min(screenX + 12, screen.getMaxX() - width - MARGIN));
        double top = screenY + 20;
        if (top + height > screen.getMaxY() - MARGIN) {
            top = Math.max(screen.getMinY() + MARGIN, screenY - height - 8);
        }
        host.move(left, top);
    }

    public void dismiss() {
        host.hide();
        setVisible(false);
    }

    private void style(Diagnostic diagnostic) {
        getStyleClass().removeAll(VARIANTS);
        getStyleClass().add("diag-tooltip-" + diagnostic.severity().name().toLowerCase(Locale.ROOT));
        setText(diagnostic.message());
    }
}