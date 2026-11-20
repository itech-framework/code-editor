package io.github.itech_framework.code_editor.feature;

import io.github.itech_framework.code_editor.CodeEditor;
import io.github.itech_framework.code_editor.EditorFeature;
import io.github.itech_framework.code_editor.internal.GoToLineLogic;
import io.github.itech_framework.code_editor.internal.GoToLinePanel;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public final class GoToLineFeature implements EditorFeature {

    public static final String ID = "go-to-line";

    private final EventHandler<KeyEvent> keyHandler = this::onKeyPressed;

    private final ChangeListener<String> textListener = (obs, old, value) -> validate();

    private CodeEditor editor;

    private GoToLinePanel panel;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void install(CodeEditor target) {
        editor = target;
        panel = target.goToLinePanel();
        target.addEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
        panel.textProperty().addListener(textListener);
        panel.setOnSubmit(this::submit);
    }

    @Override
    public void uninstall(CodeEditor target) {
        target.removeEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
        panel.textProperty().removeListener(textListener);
        panel.setOnSubmit(null);
        panel.setStatus("", false);
        target.setGoToLineVisible(false);
        editor = null;
        panel = null;
    }

    private void onKeyPressed(KeyEvent event) {
        CodeEditor target = editor;
        if (target == null || !target.isTextAreaEvent(event)) {
            return;
        }
        if (event.getCode() == KeyCode.G && event.isControlDown() && !event.isAltDown() && !event.isShiftDown()
                && !event.isMetaDown()) {
            open();
            event.consume();
        }
    }

    private void open() {
        int count = editor.getLineCount();
        int current = editor.getLineOfOffset(editor.getCaretPosition()) + 1;
        panel.setPrompt("Line 1-" + count + " (now " + current + ")");
        panel.textProperty().set("");
        panel.setStatus("", false);
        if (editor.isGoToLineVisible()) {
            panel.focusField();
        } else {
            editor.setGoToLineVisible(true);
        }
    }

    private void validate() {
        if (editor == null) {
            return;
        }
        GoToLineLogic.Result result = GoToLineLogic.parse(panel.textProperty().get(), editor.getLineCount());
        panel.setStatus(result.error() == null ? "" : result.error(), result.error() != null);
    }

    private void submit() {
        CodeEditor target = editor;
        if (target == null) {
            return;
        }
        GoToLineLogic.Result result = GoToLineLogic.parse(panel.textProperty().get(), target.getLineCount());
        if (result.isEmpty()) {
            return;
        }
        if (result.error() != null) {
            panel.setStatus(result.error(), true);
            return;
        }
        target.setGoToLineVisible(false);
        target.goToLine(result.target().line(), result.target().column());
    }
}