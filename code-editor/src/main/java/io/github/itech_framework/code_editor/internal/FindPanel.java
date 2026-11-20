package io.github.itech_framework.code_editor.internal;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import javafx.scene.layout.VBox;

public final class FindPanel extends HBox {

    private static final String ERROR_CLASS = "find-error";

    private final TextField field = new TextField();

    private final TextField replaceField = new TextField();

    private final ToggleButton expandToggle = new ToggleButton();

    private final ToggleButton caseToggle = toggle("Aa", "Match case");

    private final ToggleButton wordToggle = toggle("ab", "Match whole word");

    private final ToggleButton regexToggle = toggle(".*", "Use regular expression");

    private final Label status = new Label();

    private final Tooltip statusTip = new Tooltip();

    private final BooleanProperty replaceEnabled = new SimpleBooleanProperty(true);

    private Runnable onNext = () -> {
    };

    private Runnable onPrevious = () -> {
    };

    private Runnable onClose = () -> {
    };

    private Runnable onReplace = () -> {
    };

    private Runnable onReplaceAll = () -> {
    };

    public FindPanel() {
        getStyleClass().add("find-panel");
        field.getStyleClass().add("find-field");
        field.setPromptText("Find");
        field.setPrefColumnCount(20);
        replaceField.getStyleClass().add("find-field");
        replaceField.setPromptText("Replace");
        replaceField.setPrefColumnCount(20);
        status.getStyleClass().add("find-status");

        Region expandIcon = new Region();
        expandIcon.getStyleClass().addAll("find-icon", "find-icon-right");
        expandToggle.getStyleClass().add("find-expand");
        expandToggle.setGraphic(expandIcon);
        expandToggle.setFocusTraversable(false);
        expandToggle.setTooltip(new Tooltip("Toggle replace"));
        expandToggle.visibleProperty().bind(replaceEnabled);
        expandToggle.managedProperty().bind(replaceEnabled);

        Button previous = iconButton("find-icon-up", "Previous match (Shift+Enter)", () -> onPrevious.run());
        Button next = iconButton("find-icon-down", "Next match (Enter)", () -> onNext.run());
        Button close = iconButton("find-icon-close", "Close (Escape)", () -> onClose.run());
        HBox findRow = new HBox(field, caseToggle, wordToggle, regexToggle, status, previous, next, close);
        findRow.getStyleClass().add("find-row");

        Button replace = textButton("Replace", "Replace (Enter)", () -> onReplace.run());
        Button replaceAll = textButton("Replace all", "Replace all (Ctrl+Alt+Enter)", () -> onReplaceAll.run());
        HBox replaceRow = new HBox(replaceField, replace, replaceAll);
        replaceRow.getStyleClass().add("find-row");
        replaceRow.visibleProperty().bind(expandToggle.selectedProperty().and(replaceEnabled));
        replaceRow.managedProperty().bind(replaceRow.visibleProperty());

        VBox rows = new VBox(findRow, replaceRow);
        rows.getStyleClass().add("find-rows");
        getChildren().addAll(expandToggle, rows);

        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setVisible(false);
        managedProperty().bind(visibleProperty());
        field.addEventFilter(KeyEvent.KEY_PRESSED, this::onFindKeyPressed);
        replaceField.addEventFilter(KeyEvent.KEY_PRESSED, this::onReplaceKeyPressed);
        visibleProperty().addListener((obs, old, visible) -> {
            if (visible) {
                Platform.runLater(this::focusField);
            }
        });
    }

    public StringProperty queryProperty() {
        return field.textProperty();
    }

    public StringProperty replaceProperty() {
        return replaceField.textProperty();
    }

    public BooleanProperty replaceVisibleProperty() {
        return expandToggle.selectedProperty();
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

    public void setReplaceEnabled(boolean enabled) {
        replaceEnabled.set(enabled);
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

    public void setOnReplace(Runnable action) {
        onReplace = action == null ? () -> {
        } : action;
    }

    public void setOnReplaceAll(Runnable action) {
        onReplaceAll = action == null ? () -> {
        } : action;
    }

    public void focusField() {
        field.requestFocus();
        field.selectAll();
    }

    private void onFindKeyPressed(KeyEvent event) {
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

    private void onReplaceKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        if (code == KeyCode.ENTER) {
            if (event.isControlDown() && event.isAltDown()) {
                onReplaceAll.run();
            } else {
                onReplace.run();
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

    private static Button textButton(String text, String tip, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("find-text-button");
        button.setFocusTraversable(false);
        button.setTooltip(new Tooltip(tip));
        button.setOnAction(event -> action.run());
        return button;
    }
}