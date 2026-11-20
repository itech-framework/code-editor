package io.github.itech_framework.code_editor.feature;

import io.github.itech_framework.code_editor.CodeEditor;
import io.github.itech_framework.code_editor.EditorFeature;
import io.github.itech_framework.code_editor.internal.IndentLogic;
import javafx.event.EventHandler;
import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public final class AutoIndentFeature implements EditorFeature {

    public static final String ID = "auto-indent";

    private final EventHandler<KeyEvent> handler = this::onKeyPressed;

    private CodeEditor editor;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void install(CodeEditor target) {
        editor = target;
        target.addEventFilter(KeyEvent.KEY_PRESSED, handler);
    }

    @Override
    public void uninstall(CodeEditor target) {
        target.removeEventFilter(KeyEvent.KEY_PRESSED, handler);
        editor = null;
    }

    private void onKeyPressed(KeyEvent event) {
        CodeEditor target = editor;
        if (target == null || event.getCode() != KeyCode.ENTER || !target.isEditable()) {
            return;
        }
        if (event.isShiftDown() || event.isControlDown() || event.isAltDown() || event.isMetaDown()) {
            return;
        }

        IndexRange selection = target.getSelection();
        int start = selection.getStart();
        int end = selection.getEnd();
        int startLine = target.getLineOfOffset(start);
        int endLine = target.getLineOfOffset(end);
        String lineText = target.getLineText(startLine);
        int column = start - target.getLineStart(startLine);
        String endText = target.getLineText(endLine);
        String after = endText.substring(end - target.getLineStart(endLine));

        IndentLogic.Result result = IndentLogic.enter(lineText, column, after, target.getIndentUnit());
        target.replaceText(start, end + result.consumedAfter(), result.insertion());
        target.moveCaret(start + result.caretOffset());
        event.consume();
    }
}