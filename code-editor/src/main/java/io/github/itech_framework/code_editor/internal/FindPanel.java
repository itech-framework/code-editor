package io.github.itech_framework.code_editor.internal;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

public final class FindPanel extends HBox {

    private static final String ERROR_CLASS = "find-error";

    private final TextField field = new TextField();

    private final ToggleButton caseToggle = toggle("Aa", "Match case");

    private final ToggleButton wordToggle = toggle("ab", "Match whole word");

    private final ToggleButton regexToggle = toggle(".*", "Use regular expression");

    private final Label status = new Label();

    private final Tooltip statusTip = new Tooltip();

    private Runnable onNext = () -> {
    };

    private Runnable onPrevious = () -> {
    };

    private Runnable onClose = () -> {
    };

    public FindPanel() {
        getStyleClass().add("find-panel");
        field.getStyleClass().add("find-field");
        field.setPromptText("Find");
        field.setPrefColumnCount(20);
        status.getStyleClass().add("find-status");
        Button previous = iconButton("find-icon-up", "Previous match (Shift+Enter)", () -> onPrevious.run());
        Button next = iconButton("find-icon-down", "Next match (Enter)", () -> onNext.run());
        Button close = iconButton("find-icon-close", "Close (Escape)", () -> onClose.run());
        getChildren().addAll(field, caseToggle, wordToggle, regexToggle, status, previous, next, close);
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setVisible(false);
        managedProperty().bind(visibleProperty());
        field.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
        visibleProperty().addListener((obs, old, visible) -> {
            if (visible) {
                Platform.runLater(this::focusField);
            }
        });
    }

    public StringProperty queryProperty() {
        return field.textProperty();
    }

    public BooleanProperty caseSensitiveProperty() {
        return caseToggle.selectedProperty();
    }

    public BooleanProperty wholeWordProperty() {
        return wordToggle.selectedProperty();
    }

    public BooleanProperty regexProperty() {
        return regexToggle.selectedProperty();
    }

    public void setStatus(String text, boolean error) {
        String value = text == null ? "" : text;
        status.setText(value);
        statusTip.setText(value);
        status.setTooltip(value.isEmpty() ? null : statusTip);
        if (error) {
            if (!status.getStyleClass().contains(ERROR_CLASS)) {
                status.getStyleClass().add(ERROR_CLASS);
            }
        } else {
            status.getStyleClass().remove(ERROR_CLASS);
        }
    }

    public void setOnNext(Runnable action) {
        onNext = action == null ? () -> {
        } : action;
    }

    public void setOnPrevious(Runnable action) {
        onPrevious = action == null ? () -> {
        } : action;
    }

    public void setOnClose(Runnable action) {
        onClose = action == null ? () -> {
        } : action;
    }

    public void focusField() {
        field.requestFocus();
        field.selectAll();
    }

    private void onKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        if (code == KeyCode.ENTER || code == KeyCode.F3) {
            if (event.isShiftDown()) {
                onPrevious.run();
            } else {
                onNext.run();
            }
            event.consume();
        } else if (code == KeyCode.ESCAPE) {
            onClose.run();
            event.consume();
        }
    }

    private static ToggleButton toggle(String text, String tip) {
        ToggleButton button = new ToggleButton(text);
        button.getStyleClass().add("find-toggle");
        button.setFocusTraversable(false);
        button.setTooltip(new Tooltip(tip));
        return button;
    }

    private static Button iconButton(String icon, String tip, Runnable action) {
        Region graphic = new Region();
        graphic.getStyleClass().addAll("find-icon", icon);
        Button button = new Button();
        button.getStyleClass().add("find-button");
        button.setGraphic(graphic);
        button.setFocusTraversable(false);
        button.setTooltip(new Tooltip(tip));
        button.setOnAction(event -> action.run());
        return button;
    }
}