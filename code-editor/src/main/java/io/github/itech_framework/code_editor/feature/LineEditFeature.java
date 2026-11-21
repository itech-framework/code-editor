package io.github.itech_framework.code_editor.feature;

import io.github.itech_framework.code_editor.CodeEditor;
import io.github.itech_framework.code_editor.EditorFeature;
import io.github.itech_framework.code_editor.internal.KeyChords;
import io.github.itech_framework.code_editor.internal.LineOpsLogic;
import javafx.event.EventHandler;
import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public final class LineEditFeature implements EditorFeature {

    public static final String ID = "line-edit";

    private final EventHandler<KeyEvent> keyHandler = this::onKeyPressed;

    private CodeEditor editor;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void install(CodeEditor target) {
        editor = target;
        target.addEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
    }

    @Override
    public void uninstall(CodeEditor target) {
        target.removeEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
        editor = null;
    }

    private void onKeyPressed(KeyEvent event) {
        CodeEditor target = editor;
        if (target == null || !target.isEditable() || !target.isTextAreaEvent(event)) {
            return;
        }
        KeyCode code = event.getCode();
        boolean vertical = code == KeyCode.UP || code == KeyCode.DOWN;
        IndexRange selection = target.getSelection();
        int start = selection.getStart();
        int end = selection.getEnd();
        LineOpsLogic.Edit edit;
        if (vertical && event.isAltDown() && !event.isControlDown() && !event.isMetaDown()) {
            String text = target.getText();
            boolean up = code == KeyCode.UP;
            if (event.isShiftDown()) {
                edit = up ? LineOpsLogic.duplicateUp(text, start, end) : LineOpsLogic.duplicateDown(text, start, end);
            } else {
                edit = up ? LineOpsLogic.moveUp(text, start, end) : LineOpsLogic.moveDown(text, start, end);
            }
        } else if (code == KeyCode.K && event.isShortcutDown() && event.isShiftDown() && !event.isAltDown()) {
            edit = LineOpsLogic.deleteLines(target.getText(), start, end);
        } else if (event.isShortcutDown() && !event.isAltDown()
                && KeyChords.isCommentToggle(code.name(), event.getText(), event.isShiftDown())) {
            String prefix = target.getLanguageDefinition().getLineComment().orElse(null);
            if (prefix == null) {
                return;
            }
            edit = LineOpsLogic.toggleComment(target.getText(), start, end, prefix);
        } else {
            return;
        }
        event.consume();
        if (edit == null) {
            return;
        }
        target.replaceText(edit.start(), edit.end(), edit.text());
        target.selectRange(edit.selectionStart(), edit.selectionEnd());
        target.revealSelection();
    }
}