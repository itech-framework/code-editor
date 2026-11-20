package io.github.itech_framework.code_editor.feature;

import io.github.itech_framework.code_editor.CodeEditor;
import io.github.itech_framework.code_editor.EditorFeature;
import io.github.itech_framework.code_editor.internal.TabLogic;
import javafx.event.EventHandler;
import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public final class TabFeature implements EditorFeature {

    public static final String ID = "tab-indent";

    private final EventHandler<KeyEvent> pressedHandler = this::onKeyPressed;

    private final EventHandler<KeyEvent> typedHandler = this::onKeyTyped;

    private CodeEditor editor;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void install(CodeEditor target) {
        editor = target;
        target.addEventFilter(KeyEvent.KEY_PRESSED, pressedHandler);
        target.addEventFilter(KeyEvent.KEY_TYPED, typedHandler);
    }

    @Override
    public void uninstall(CodeEditor target) {
        target.removeEventFilter(KeyEvent.KEY_PRESSED, pressedHandler);
        target.removeEventFilter(KeyEvent.KEY_TYPED, typedHandler);
        editor = null;
    }

    private void onKeyTyped(KeyEvent event) {
        CodeEditor target = editor;
        if (target == null || !target.isEditable() || !target.isTextAreaEvent(event) || !"\t".equals(event.getCharacter())) {
            return;
        }
        if (event.isControlDown() || event.isAltDown() || event.isMetaDown()) {
            return;
        }
        event.consume();
    }

    private void onKeyPressed(KeyEvent event) {
        CodeEditor target = editor;
        if (target == null || event.getCode() != KeyCode.TAB || !target.isEditable() || !target.isTextAreaEvent(event)) {
            return;
        }
        if (event.isControlDown() || event.isAltDown() || event.isMetaDown()) {
            return;
        }
        if (event.isShiftDown()) {
            applyBlock(target, false);
        } else {
            indent(target);
        }
        event.consume();
    }

    private void indent(CodeEditor target) {
        IndexRange selection = target.getSelection();
        int start = selection.getStart();
        int end = selection.getEnd();
        int startLine = target.getLineOfOffset(start);
        if (startLine != target.getLineOfOffset(end)) {
            applyBlock(target, true);
            return;
        }
        String lineText = target.getLineText(startLine);
        int column = start - target.getLineStart(startLine);
        String text = TabLogic.insertion(lineText, column, target.getTabSize(), target.isInsertSpaces());
        target.replaceText(start, end, text);
        target.moveCaret(start + text.length());
    }

    private void applyBlock(CodeEditor target, boolean indent) {
        IndexRange selection = target.getSelection();
        int start = selection.getStart();
        int end = selection.getEnd();
        int startLine = target.getLineOfOffset(start);
        int endLine = target.getLineOfOffset(end);
        boolean excluded = endLine > startLine && end == target.getLineStart(endLine);
        if (excluded) {
            endLine--;
        }
        int blockStart = target.getLineStart(startLine);
        int blockEnd = target.getLineStart(endLine) + target.getLineText(endLine).length();
        String text = target.getText(blockStart, blockEnd);
        int relativeStart = start - blockStart;
        int relativeEnd = Math.min(end, blockEnd) - blockStart;

        TabLogic.Block result = indent
                ? TabLogic.indent(text, relativeStart, relativeEnd, target.getIndentUnit())
                : TabLogic.outdent(text, relativeStart, relativeEnd, target.getTabSize());

        if (!result.text().equals(text)) {
            target.replaceText(blockStart, blockEnd, result.text());
        }
        int selectionEnd = excluded ? blockStart + result.text().length() + 1 : blockStart + result.end();
        target.selectRange(blockStart + result.start(), selectionEnd);
    }
}