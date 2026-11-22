package io.github.itech_framework.code_editor.internal;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Window;

public final class FloatingHost {

    public static final double SHADOW = 10;

    private final Node content;

    private Runnable onMoved = null;

    private final Popup window = new Popup();

    private final StackPane root = new StackPane();

    private final ChangeListener<Number> moveListener = (obs, old, value) -> onMoved.run();

    private Window watched;

    public FloatingHost(Node content, Runnable onMoved) {
        this.content = content;
        this.onMoved = onMoved;
        root.getStyleClass().addAll("code-editor", "floating-host");
        root.setPadding(new Insets(SHADOW));
        root.setPickOnBounds(false);
        window.getContent().add(root);
        window.setAutoFix(false);
        window.setAutoHide(false);
        window.setConsumeAutoHidingEvents(false);
    }

    public static Rectangle2D screenBounds(double x, double y) {
        List<Screen> screens = Screen.getScreensForRectangle(x, y, 1, 1);
        Screen screen = screens.isEmpty() ? Screen.getPrimary() : screens.get(0);
        return screen.getVisualBounds();
    }

    public void show(Control owner, double x, double y) {
        Window ownerWindow = owner.getScene() == null ? null : owner.getScene().getWindow();
        if (ownerWindow == null) {
            return;
        }
        if (content.getParent() != root) {
            root.getChildren().setAll(content);
        }
        List<String> sheets = new ArrayList<>();
        String base = owner.getUserAgentStylesheet();
        if (base != null) {
            sheets.add(base);
        }
        sheets.addAll(owner.getStylesheets());
        if (!root.getStylesheets().equals(sheets)) {
            root.getStylesheets().setAll(sheets);
        }
        watch(ownerWindow);
        if (window.isShowing()) {
            move(x, y);
        } else {
            window.show(ownerWindow, x - SHADOW, y - SHADOW);
        }
        root.applyCss();
    }

    public void move(double x, double y) {
        window.setX(x - SHADOW);
        window.setY(y - SHADOW);
    }

    public void hide() {
        unwatch();
        if (window.isShowing()) {
            window.hide();
        }
    }

    private void watch(Window ownerWindow) {
        if (watched == ownerWindow) {
            return;
        }
        unwatch();
        ownerWindow.xProperty().addListener(moveListener);
        ownerWindow.yProperty().addListener(moveListener);
        ownerWindow.widthProperty().addListener(moveListener);
        ownerWindow.heightProperty().addListener(moveListener);
        watched = ownerWindow;
    }

    private void unwatch() {
        if (watched == null) {
            return;
        }
        watched.xProperty().removeListener(moveListener);
        watched.yProperty().removeListener(moveListener);
        watched.widthProperty().removeListener(moveListener);
        watched.heightProperty().removeListener(moveListener);
        watched = null;
    }
}