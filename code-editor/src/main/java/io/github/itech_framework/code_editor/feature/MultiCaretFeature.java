package io.github.itech_framework.code_editor.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.github.itech_framework.code_editor.CodeEditor;
import io.github.itech_framework.code_editor.EditorFeature;
import io.github.itech_framework.code_editor.internal.MultiEditLogic;
import javafx.event.EventHandler;
import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public final class MultiCaretFeature implements EditorFeature {

    public static final String ID = "multi-caret";

    private static final Set<KeyCode> MODIFIERS = Set.of(KeyCode.SHIFT, KeyCode.CONTROL, KeyCode.ALT,
            KeyCode.ALT_GRAPH, KeyCode.META, KeyCode.WINDOWS, KeyCode.COMMAND, KeyCode.CAPS);

    private final EventHandler<MouseEvent> pressedHandler = this::onMousePressed;

    private final EventHandler<MouseEvent> followUpHandler = this::onMouseFollowUp;

    private final EventHandler<KeyEvent> keyHandler = this::onKeyPressed;

    private final EventHandler<KeyEvent> typedHandler = this::onKeyTyped;

    private CodeEditor editor;

    private boolean blocking;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void install(CodeEditor target) {
        editor = target;
        target.addEventFilter(MouseEvent.MOUSE_PRESSED, pressedHandler);
        target.addEventFilter(MouseEvent.MOUSE_DRAGGED, followUpHandler);
        target.addEventFilter(MouseEvent.MOUSE_RELEASED, followUpHandler);
        target.addEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
        target.addEventFilter(KeyEvent.KEY_TYPED, typedHandler);
    }

    @Override
    public void uninstall(CodeEditor target) {
        target.removeEventFilter(MouseEvent.MOUSE_PRESSED, pressedHandler);
        target.removeEventFilter(MouseEvent.MOUSE_DRAGGED, followUpHandler);
        target.removeEventFilter(MouseEvent.MOUSE_RELEASED, followUpHandler);
        target.removeEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
        target.removeEventFilter(KeyEvent.KEY_TYPED, typedHandler);
        target.clearExtraCarets();
        blocking = false;
        editor = null;
    }

    private void onMousePressed(MouseEvent event) {
        CodeEditor target = editor;
        if (target == null || event.getButton() != MouseButton.PRIMARY || !target.isTextAreaEvent(event)) {
            return;
        }
        if (event.isAltDown() && !event.isShortcutDown() && !event.isShiftDown()) {
            target.requestFocus();
            target.toggleExtraCaret(target.offsetAt(event.getScreenX(), event.getScreenY()));
            blocking = true;
            event.consume();
            return;
        }
        target.clearExtraCarets();
    }

    private void onMouseFollowUp(MouseEvent event) {
        if (!blocking) {
            return;
        }
        event.consume();
        if (event.getEventType() == MouseEvent.MOUSE_RELEASED) {
            blocking = false;
        }
    }

    private void onKeyPressed(KeyEvent event) {
        CodeEditor target = editor;
        if (target == null || !target.isTextAreaEvent(event)) {
            return;
        }
        KeyCode code = event.getCode();
        if (MODIFIERS.contains(code)) {
            return;
        }
        boolean vertical = code == KeyCode.UP || code == KeyCode.DOWN;
        if (vertical && event.isShortcutDown() && event.isAltDown() && !event.isShiftDown()) {
            addVertically(target, code == KeyCode.UP ? -1 : 1);
            event.consume();
            return;
        }
        if (code == KeyCode.ESCAPE) {
            if (!event.isConsumed() && target.hasExtraCarets() && !target.isFindPanelVisible()
                    && !target.isGoToLineVisible()) {
                target.clearExtraCarets();
                event.consume();
            }
            return;
        }
        if (target.hasExtraCarets() && target.isEditable() && isTypingChord(event)) {
            if (code == KeyCode.BACK_SPACE) {
                apply(target, MultiEditLogic.backspace(target.getText(), spans(target), 0));
                event.consume();
                return;
            }
            if (code == KeyCode.DELETE && !event.isShiftDown()) {
                apply(target, MultiEditLogic.delete(target.getText(), spans(target), 0));
                event.consume();
                return;
            }
            if (isTypingKey(code)) {
                return;
            }
        }
        target.clearExtraCarets();
    }

    private void onKeyTyped(KeyEvent event) {
        CodeEditor target = editor;
        if (target == null || !target.hasExtraCarets() || !target.isEditable() || !target.isTextAreaEvent(event)
                || !isTypingChord(event)) {
            return;
        }
        String text = event.getCharacter();
        if (text.isEmpty() || Character.isISOControl(text.charAt(0))) {
            return;
        }
        apply(target, MultiEditLogic.insert(target.getText(), spans(target), 0, text));
        event.consume();
    }

    private static boolean isTypingChord(KeyEvent event) {
        if (event.isMetaDown()) {
            return false;
        }
        if (event.isControlDown()) {
            return event.isAltDown();
        }
        return !event.isAltDown();
    }

    private static boolean isTypingKey(KeyCode code) {
        return code != KeyCode.ENTER && code != KeyCode.TAB && !code.isNavigationKey() && !code.isArrowKey()
                && !code.isFunctionKey() && !code.isMediaKey();
    }

    private static List<MultiEditLogic.Span> spans(CodeEditor target) {
        IndexRange selection = target.getSelection();
        List<MultiEditLogic.Span> spans = new ArrayList<>();
        spans.add(new MultiEditLogic.Span(selection.getStart(), selection.getEnd()));
        List<Integer> positions = target.getCaretPositions();
        for (int i = 1; i < positions.size(); i++) {
            spans.add(new MultiEditLogic.Span(positions.get(i), positions.get(i)));
        }
        return spans;
    }

    private static void apply(CodeEditor target, MultiEditLogic.Plan plan) {
        target.applyEdits(plan.edits());
        List<Integer> carets = new ArrayList<>(plan.carets());
        int primary = carets.remove(plan.primary());
        target.setCaretPositions(primary, carets);
    }

    private void addVertically(CodeEditor target, int direction) {
        List<Integer> positions = target.getCaretPositions();
        int reference = direction < 0 ? Collections.min(positions) : Collections.max(positions);
        int line = target.getLineOfOffset(reference);
        int column = reference - target.getLineStart(line);
        int next = line + direction;
        while (next >= 0 && next < target.getLineCount() && target.isLineHidden(next)) {
            next += direction;
        }
        if (next < 0 || next >= target.getLineCount()) {
            return;
        }
        int offset = target.getLineStart(next) + Math.min(column, target.getLineText(next).length());
        target.addExtraCaret(offset);
    }
}