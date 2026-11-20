package io.github.itech_framework.code_editor.internal;

import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

import org.fxmisc.richtext.CodeArea;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public final class LineNumberGutter implements IntFunction<Node> {

    public static final String STYLE_CLASS = "code-editor-line-number";

    public static final String FOLD_STYLE_CLASS = "code-editor-fold";

    private static final int MIN_DIGITS = 3;

    private static final double FOLD_WIDTH = 22;

    private static final String EXPANDED = "\u25BE";

    private static final String COLLAPSED = "\u25B8";

    private final CodeArea area;

    private final FoldState folds;

    private final IntPredicate hidden;

    private final IntConsumer toggle;

    private final Val<Integer> paragraphCount;

    public LineNumberGutter(CodeArea area, FoldState folds, IntPredicate hidden, IntConsumer toggle) {
        this.area = area;
        this.folds = folds;
        this.hidden = hidden;
        this.toggle = toggle;
        this.paragraphCount = LiveList.sizeOf(area.getParagraphs());
    }

    @Override
    public Node apply(int index) {
        Label number = new Label();
        number.getStyleClass().add(STYLE_CLASS);
        number.setAlignment(Pos.TOP_RIGHT);
        number.setMaxWidth(Double.MAX_VALUE);
        number.setMaxHeight(Double.MAX_VALUE);
        Val<String> text = paragraphCount.map(count -> format(index + 1, count));
        number.textProperty().bind(text.conditionOnShowing(number));

        Label arrow = new Label();
        arrow.getStyleClass().add(FOLD_STYLE_CLASS);
        arrow.setAlignment(Pos.TOP_CENTER);
        arrow.setMinWidth(FOLD_WIDTH);
        arrow.setPrefWidth(FOLD_WIDTH);
        arrow.setMaxWidth(FOLD_WIDTH);
        arrow.setMaxHeight(Double.MAX_VALUE);
        Val<String> mark = Val.wrap(folds.revisionProperty()).map(revision -> markFor(index));
        Val<String> shown = mark.conditionOnShowing(arrow);
        arrow.textProperty().bind(shown);
        arrow.cursorProperty().bind(shown.map(value -> value == null || value.isEmpty() ? Cursor.DEFAULT : Cursor.HAND));
        arrow.setOnMouseClicked(event -> {
            String value = arrow.getText();
            if (event.getButton() == MouseButton.PRIMARY && value != null && !value.isEmpty()) {
                toggle.accept(index);
                event.consume();
            }
        });

        HBox box = new HBox(number, arrow);
        HBox.setHgrow(number, Priority.ALWAYS);
        Val<Boolean> visible = Val.wrap(folds.revisionProperty()).map(revision -> !hidden.test(index));
        box.visibleProperty().bind(visible.conditionOnShowing(box).orElseConst(Boolean.TRUE));
        return box;
    }

    private String markFor(int index) {
        FoldLogic.Range range = FoldLogic.startingAt(folds.getRanges(), index);
        if (range == null || range.endLine() >= area.getParagraphs().size()) {
            return "";
        }
        return hidden.test(range.firstHiddenLine()) ? COLLAPSED : EXPANDED;
    }

    private static String format(int number, int count) {
        int digits = Math.max(MIN_DIGITS, String.valueOf(count).length());
        return String.format(Locale.ROOT, "%" + digits + "d", number);
    }
}