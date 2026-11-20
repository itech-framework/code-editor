package io.github.itech_framework.code_editor.internal;

import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class GoToLinePanel extends HBox {

    private static final String ERROR_CLASS = "find-error";

    private final TextField field = new TextField();

    private final Label status = new Label();

    private Runnable onSubmit = () -> {
    };

    private Runnable onClose = () -> {
    };

    public GoToLinePanel() {
        getStyleClass().addAll("find-panel", "goto-panel");
        field.getStyleClass().add("find-field");
        field.setPrefColumnCount(16);
        status.getStyleClass().add("find-status");

        Region graphic = new Region();
        graphic.getStyleClass().addAll("find-icon", "find-icon-close");
        Button close = new Button();
        close.getStyleClass().add("find-button");
        close.setGraphic(graphic);
        close.setFocusTraversable(false);
        close.setTooltip(new Tooltip("Close (Escape)"));
        close.setOnAction(event -> onClose.run());

        HBox row = new HBox(field, status, close);
        row.getStyleClass().add("find-row");
        VBox rows = new VBox(row);
        rows.getStyleClass().add("find-rows");
        getChildren().add(rows);

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

    public StringProperty textProperty() {
        return field.textProperty();
    }

    public void setPrompt(String text) {
        field.setPromptText(text);
    }

    public void setStatus(String text, boolean error) {
        status.setText(text == null ? "" : text);
        if (error) {
            if (!status.getStyleClass().contains(ERROR_CLASS)) {
                status.getStyleClass().add(ERROR_CLASS);
            }
        } else {
            status.getStyleClass().remove(ERROR_CLASS);
        }
    }

    public void setOnSubmit(Runnable action) {
        onSubmit = action == null ? () -> {
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
        if (code == KeyCode.ENTER) {
            onSubmit.run();
            event.consume();
        } else if (code == KeyCode.ESCAPE) {
            onClose.run();
            event.consume();
        }
    }
}