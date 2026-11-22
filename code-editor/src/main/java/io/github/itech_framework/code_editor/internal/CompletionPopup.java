package io.github.itech_framework.code_editor.internal;

import java.util.List;
import java.util.function.Consumer;

import io.github.itech_framework.code_editor.completion.CompletionItem;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public final class CompletionPopup extends ListView<CompletionItem> {

    public static final int MAX_ROWS = 8;

    private static final double ROW_HEIGHT = 22;

    private static final double MARGIN = 4;

    private static final double GAP = 2;

    private static final double MIN_WIDTH = 180;

    private static final double MAX_WIDTH = 460;

    private static final double EXTRA_WIDTH = 64;

    private static final String[] KIND_CLASSES = { "completion-kind-keyword", "completion-kind-word",
            "completion-kind-variable" };

    private int top;

    private Consumer<CompletionItem> acceptHandler;
    
    private final FloatingHost host = new FloatingHost(this, this::dismiss);

    public CompletionPopup() {
        getStyleClass().add("completion-popup");
        setFocusTraversable(false);
        setFixedCellSize(ROW_HEIGHT);
        setCellFactory(list -> new Row());
        setVisible(false);
        managedProperty().bind(visibleProperty());
    }

    public void setOnAccept(Consumer<CompletionItem> handler) {
        this.acceptHandler = handler;
    }

    public void show(List<CompletionItem> items, int selectedIndex, double x, double yBelow, double yAbove,
            double boundsWidth, double boundsHeight) {
        double[] size = prepare(items);
        double width = size[0];
        double height = size[1];
        double left = x;
        if (left + width > boundsWidth - MARGIN) {
            left = Math.max(MARGIN, boundsWidth - width - MARGIN);
        }
        double topPosition = yBelow + GAP;
        if (topPosition + height > boundsHeight - MARGIN) {
            topPosition = Math.max(MARGIN, yAbove - height - GAP);
        }
        setTranslateX(left);
        setTranslateY(topPosition);
        setVisible(true);
        applyCss();
        layout();
        top = 0;
        select(selectedIndex);
    }

    public void showInWindow(Control owner, List<CompletionItem> items, int selectedIndex, double screenX,
            double screenBelow, double screenAbove) {
        double[] size = prepare(items);
        Rectangle2D screen = FloatingHost.screenBounds(screenX, screenBelow);
        double left = Math.max(screen.getMinX() + MARGIN,
                Math.min(screenX, screen.getMaxX() - size[0] - MARGIN));
        double topPosition = screenBelow + GAP;
        if (topPosition + size[1] > screen.getMaxY() - MARGIN) {
            topPosition = Math.max(screen.getMinY() + MARGIN, screenAbove - size[1] - GAP);
        }
        setTranslateX(0);
        setTranslateY(0);
        setVisible(true);
        host.show(owner, left, topPosition);
        applyCss();
        layout();
        top = 0;
        select(selectedIndex);
    }

    public void dismiss() {
        host.hide();
        setVisible(false);
        getItems().clear();
        top = 0;
    }

    private double[] prepare(List<CompletionItem> items) {
        getItems().setAll(items);
        int rows = Math.min(MAX_ROWS, items.size());
        double width = measureWidth(items);
        double height = rows * ROW_HEIGHT + 2;
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
        return new double[] { width, height };
    }

    public void move(int delta) {
        select(getSelectionModel().getSelectedIndex() + delta);
    }

    public CompletionItem selectedItem() {
        return getSelectionModel().getSelectedItem();
    }

    private void select(int index) {
        int size = getItems().size();
        if (size == 0) {
            return;
        }
        int target = Math.floorMod(index, size);
        int rows = Math.min(MAX_ROWS, size);
        if (target < top) {
            top = target;
        } else if (target >= top + rows) {
            top = target - rows + 1;
        }
        getSelectionModel().select(target);
        scrollTo(top);
    }

    private static double measureWidth(List<CompletionItem> items) {
        String longest = "";
        for (CompletionItem item : items) {
            if (item.label().length() > longest.length()) {
                longest = item.label();
            }
        }
        Text text = new Text(longest);
        text.setFont(Font.font(12));
        double width = text.getLayoutBounds().getWidth() + EXTRA_WIDTH;
        return Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, width));
    }

    private static String kindClass(CompletionItem.Kind kind) {
        return KIND_CLASSES[kind.ordinal()];
    }

    private static String kindLetter(CompletionItem.Kind kind) {
        return switch (kind) {
        case KEYWORD -> "K";
        case WORD -> "W";
        case VARIABLE -> "V";
        };
    }

    private final class Row extends ListCell<CompletionItem> {

        private final Label badge = new Label();

        private final Label name = new Label();

        private final HBox box = new HBox(8, badge, name);

        private Row() {
            box.setAlignment(Pos.CENTER_LEFT);
            badge.getStyleClass().add("completion-kind");
            name.getStyleClass().add("completion-label");
            setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1 && !isEmpty()
                        && getItem() != null && acceptHandler != null) {
                    acceptHandler.accept(getItem());
                    event.consume();
                }
            });
        }

        @Override
        protected void updateItem(CompletionItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            badge.getStyleClass().removeAll(KIND_CLASSES);
            badge.getStyleClass().add(kindClass(item.kind()));
            badge.setText(kindLetter(item.kind()));
            name.setText(item.label());
            setText(null);
            setGraphic(box);
        }
    }
}