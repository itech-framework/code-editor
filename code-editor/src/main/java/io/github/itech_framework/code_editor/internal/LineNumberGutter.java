package io.github.itech_framework.code_editor.internal;

import java.util.Locale;
import java.util.function.IntFunction;

import org.fxmisc.richtext.CodeArea;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;

public final class LineNumberGutter implements IntFunction<Node> {

    public static final String STYLE_CLASS = "code-editor-line-number";

    private static final int MIN_DIGITS = 3;

    private final Val<Integer> paragraphCount;

    public LineNumberGutter(CodeArea area) {
        this.paragraphCount = LiveList.sizeOf(area.getParagraphs());
    }

    @Override
    public Node apply(int index) {
        Label label = new Label();
        label.getStyleClass().add(STYLE_CLASS);
        label.setAlignment(Pos.TOP_RIGHT);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMaxHeight(Double.MAX_VALUE);
        Val<String> text = paragraphCount.map(count -> format(index + 1, count));
        label.textProperty().bind(text.conditionOnShowing(label));
        return label;
    }

    private static String format(int number, int count) {
        int digits = Math.max(MIN_DIGITS, String.valueOf(count).length());
        return String.format(Locale.ROOT, "%" + digits + "d", number);
    }
}