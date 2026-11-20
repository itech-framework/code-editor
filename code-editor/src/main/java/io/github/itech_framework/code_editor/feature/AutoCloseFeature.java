package io.github.itech_framework.code_editor.feature;

import java.util.List;

import io.github.itech_framework.code_editor.CodeEditor;
import io.github.itech_framework.code_editor.EditorFeature;
import io.github.itech_framework.code_editor.internal.AutoCloseLogic;
import io.github.itech_framework.code_editor.language.BracketPair;
import javafx.event.EventHandler;
import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public final class AutoCloseFeature implements EditorFeature {

    public static final String ID = "auto-close";

    private final EventHandler<KeyEvent> typedHandler = this::onKeyTyped;

    private final EventHandler<KeyEvent> pressedHandler = this::onKeyPressed;

    private CodeEditor editor;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void install(CodeEditor target) {
        editor = target;
        target.addEventFilter(KeyEvent.KEY_TYPED, typedHandler);
        target.addEventFilter(KeyEvent.KEY_PRESSED, pressedHandler);
    }

    @Override
    public void uninstall(CodeEditor target) {
        target.removeEventFilter(KeyEvent.KEY_TYPED, typedHandler);
        target.removeEventFilter(KeyEvent.KEY_PRESSED, pressedHandler);
        editor = null;
    }

    private void onKeyTyped(KeyEvent event) {
        CodeEditor target = editor;
        if (target == null || !target.isEditable() || !target.isTextAreaEvent(event)) {
            return;
        }
        if (event.isMetaDown() || (event.isControlDown() && !event.isAltDown())) {
            return;
        }
        String text = event.getCharacter();
        if (text.length() != 1) {
            return;
        }
        char typed = text.charAt(0);
        List<BracketPair> pairs = target.getLanguageDefinition().getAutoClosingPairs();
        IndexRange selection = target.getSelection();
        int start = selection.getStart();
        int end = selection.getEnd();
        int line = target.getLineOfOffset(start);
        String lineText = target.getLineText(line);
        int column = start - target.getLineStart(line);

        AutoCloseLogic.Result result = AutoCloseLogic.typed(typed, lineText, column, end > start, pairs);
        switch (result.kind()) {
            case PAIR -> {
                target.replaceText(start, start, "" + typed + result.close());
                target.moveCaret(start + 1);
                event.consume();
            }
            case WRAP -> {
                String selected = target.getText(start, end);
                target.replaceText(start, end, typed + selected + result.close());
                target.selectRange(start + 1, end + 1);
                event.consume();
            }
            case OVERTYPE -> {
                target.moveCaret(start + 1);
                event.consume();
            }
            default -> {
            }
        }
    }

    private void onKeyPressed(KeyEvent event) {
        CodeEditor target = editor;
        if (target == null || event.getCode() != KeyCode.BACK_SPACE || !target.isEditable() || !target.isTextAreaEvent(event)) {
            return;
        }
        if (event.isShiftDown() || event.isControlDown() || event.isAltDown() || event.isMetaDown()) {
            return;
        }
        IndexRange selection = target.getSelection();
        if (selection.getLength() != 0) {
            return;
        }
        int caret = selection.getStart();
        int line = target.getLineOfOffset(caret);
        String lineText = target.getLineText(line);
        int column = caret - target.getLineStart(line);
        List<BracketPair> pairs = target.getLanguageDefinition().getAutoClosingPairs();
        if (AutoCloseLogic.deletesPair(lineText, column, pairs)) {
            target.replaceText(caret - 1, caret + 1, "");
            event.consume();
        }
    }
}