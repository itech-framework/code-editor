package io.github.itech_framework.code_editor.internal;

import java.util.function.IntConsumer;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.TwoDimensional.Bias;

import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

public final class FoldMarkers {

    public static final String MARKER_CLASS = "code-editor-fold-marker";

    private static final String TEXT = "\u2026";

    private static final double GAP = 8;

    private final CodeArea area;

    private final String headerClass;

    private final IntConsumer toggle;

    private final PauseTransition delay = new PauseTransition(Duration.millis(30));

    public FoldMarkers(CodeArea area, String headerClass, IntConsumer toggle) {
        this.area = area;
        this.headerClass = headerClass;
        this.toggle = toggle;
        delay.setOnFinished(event -> sync());
    }

    public void request() {
        if (delay.getStatus() != Animation.Status.RUNNING) {
            delay.playFromStart();
        }
    }

    private void sync() {
        for (Node node : area.lookupAll(".paragraph-text")) {
            if (!(node instanceof TextFlow flow)) {
                continue;
            }
            Label marker = find(flow);
            if (flow.getStyleClass().contains(headerClass)) {
                if (marker == null) {
                    marker = create();
                    flow.getChildren().add(marker);
                }
                place(flow, marker);
            } else if (marker != null) {
                flow.getChildren().remove(marker);
            }
        }
    }

    private Label find(TextFlow flow) {
        for (Node child : flow.getChildren()) {
            if (child instanceof Label && child.getStyleClass().contains(MARKER_CLASS)) {
                return (Label) child;
            }
        }
        return null;
    }

    private Label create() {
        Label marker = new Label(TEXT);
        marker.getStyleClass().add(MARKER_CLASS);
        marker.setManaged(false);
        marker.setFocusTraversable(false);
        marker.setCursor(Cursor.HAND);
        marker.setOnMousePressed(MouseEvent::consume);
        marker.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                Point2D point = area.screenToLocal(event.getScreenX(), event.getScreenY());
                int offset = area.hit(point.getX(), point.getY()).getInsertionIndex();
                toggle.accept(area.offsetToPosition(offset, Bias.Forward).getMajor());
                event.consume();
            }
        });
        return marker;
    }

    private void place(TextFlow flow, Label marker) {
        marker.applyCss();
        double width = marker.prefWidth(-1);
        double height = marker.prefHeight(-1);
        double flowHeight = flow.getHeight() > 0 ? flow.getHeight() : height;
        marker.resize(width, height);
        marker.relocate(flow.prefWidth(-1) + GAP, Math.max(0, (flowHeight - height) / 2));
    }
}