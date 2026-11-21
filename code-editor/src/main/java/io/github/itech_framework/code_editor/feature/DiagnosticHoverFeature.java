package io.github.itech_framework.code_editor.feature;

import java.util.List;
import java.util.OptionalInt;

import io.github.itech_framework.code_editor.CodeEditor;
import io.github.itech_framework.code_editor.EditorFeature;
import io.github.itech_framework.code_editor.diagnostics.Diagnostic;
import io.github.itech_framework.code_editor.internal.DiagnosticTooltip;
import javafx.animation.PauseTransition;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.stage.Window;
import javafx.util.Duration;

public final class DiagnosticHoverFeature implements EditorFeature {

    public static final String ID = "diagnostic-hover";

    private final PauseTransition delay = new PauseTransition(Duration.millis(400));

    private final EventHandler<MouseEvent> movedHandler = this::onMoved;

    private final EventHandler<MouseEvent> mouseDismiss = event -> hide();

    private final EventHandler<KeyEvent> keyDismiss = event -> hide();

    private final EventHandler<ScrollEvent> scrollDismiss = event -> hide();

    private final ChangeListener<Boolean> windowFocusListener = (obs, old, focused) -> {
        if (!focused) {
            hide();
        }
    };

    private final ChangeListener<Object> diagnosticsListener = (obs, old, value) -> hide();

    private CodeEditor editor;

    private DiagnosticTooltip tooltip;

    private Diagnostic shown;

    private Window watchedWindow;

    private double screenX;

    private double screenY;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void install(CodeEditor target) {
        editor = target;
        tooltip = target.diagnosticTooltip();
        delay.setOnFinished(event -> show());
        target.addEventFilter(MouseEvent.MOUSE_MOVED, movedHandler);
        target.addEventFilter(MouseEvent.MOUSE_PRESSED, mouseDismiss);
        target.addEventHandler(MouseEvent.MOUSE_EXITED, mouseDismiss);
        target.addEventFilter(KeyEvent.KEY_PRESSED, keyDismiss);
        target.addEventFilter(ScrollEvent.SCROLL, scrollDismiss);
        target.diagnosticsProperty().addListener(diagnosticsListener);
    }

    @Override
    public void uninstall(CodeEditor target) {
        target.removeEventFilter(MouseEvent.MOUSE_MOVED, movedHandler);
        target.removeEventFilter(MouseEvent.MOUSE_PRESSED, mouseDismiss);
        target.removeEventHandler(MouseEvent.MOUSE_EXITED, mouseDismiss);
        target.removeEventFilter(KeyEvent.KEY_PRESSED, keyDismiss);
        target.removeEventFilter(ScrollEvent.SCROLL, scrollDismiss);
        target.diagnosticsProperty().removeListener(diagnosticsListener);
        delay.setOnFinished(null);
        hide();
        editor = null;
        tooltip = null;
    }

    private void onMoved(MouseEvent event) {
        screenX = event.getScreenX();
        screenY = event.getScreenY();
        if (shown != null) {
            if (isOver(shown)) {
                return;
            }
            hide();
        }
        delay.playFromStart();
    }

    private boolean isOver(Diagnostic diagnostic) {
        CodeEditor target = editor;
        if (target == null) {
            return false;
        }
        OptionalInt index = target.charIndexAt(screenX, screenY);
        int[] range = DiagnosticsFeature.range(diagnostic, target.getLength());
        return index.isPresent() && range != null && index.getAsInt() >= range[0] && index.getAsInt() < range[1];
    }

    private void show() {
        CodeEditor target = editor;
        if (target == null || target.getScene() == null) {
            return;
        }
        Window window = target.getScene().getWindow();
        if (window == null || !window.isFocused()) {
            return;
        }
        OptionalInt index = target.charIndexAt(screenX, screenY);
        if (index.isEmpty()) {
            return;
        }
        Diagnostic best = pick(target.getDiagnostics(), index.getAsInt(), target.getLength());
        if (best == null || !(tooltip.getParent() instanceof Region parent)) {
            return;
        }
        Point2D local = parent.screenToLocal(screenX, screenY);
        if (local == null) {
            return;
        }
        shown = best;
        watch(window);
        tooltip.show(best, local.getX(), local.getY(), parent.getWidth(), parent.getHeight());
    }

    private static Diagnostic pick(List<Diagnostic> diagnostics, int index, int length) {
        Diagnostic best = null;
        int bestSize = Integer.MAX_VALUE;
        for (Diagnostic diagnostic : diagnostics) {
            int[] range = DiagnosticsFeature.range(diagnostic, length);
            if (range == null || index < range[0] || index >= range[1]) {
                continue;
            }
            int size = range[1] - range[0];
            if (best == null || diagnostic.severity().compareTo(best.severity()) < 0
                    || (diagnostic.severity() == best.severity() && size < bestSize)) {
                best = diagnostic;
                bestSize = size;
            }
        }
        return best;
    }

    private void hide() {
        delay.stop();
        shown = null;
        unwatch();
        if (tooltip != null) {
            tooltip.dismiss();
        }
    }

    private void watch(Window window) {
        if (watchedWindow != window) {
            unwatch();
            window.focusedProperty().addListener(windowFocusListener);
            watchedWindow = window;
        }
    }

    private void unwatch() {
        if (watchedWindow != null) {
            watchedWindow.focusedProperty().removeListener(windowFocusListener);
            watchedWindow = null;
        }
    }
}