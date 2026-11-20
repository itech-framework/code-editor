package io.github.itech_framework.code_editor.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.github.itech_framework.code_editor.CodeEditor;
import io.github.itech_framework.code_editor.EditorFeature;
import io.github.itech_framework.code_editor.internal.BracketLogic;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.util.Duration;

public final class BracketMatchFeature implements EditorFeature {

    public static final String ID = "bracket-match";

    public static final String MARK_CLASS = "bracket-match";

    private static final int WINDOW = BracketLogic.MAX_SCAN;

    private final ChangeListener<Integer> caretListener = (observable, old, value) -> schedule();

    private final List<Integer> marks = new ArrayList<>();

    private final PauseTransition idle = new PauseTransition(Duration.millis(200));

    private CodeEditor editor;

    private Runnable stopListening;

    private boolean pending;

    public BracketMatchFeature() {
        idle.setOnFinished(event -> update());
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void install(CodeEditor target) {
        editor = target;
        target.caretPositionProperty().addListener(caretListener);
        stopListening = target.addTextChangeListener(this::onTextChanged);
        schedule();
    }

    @Override
    public void uninstall(CodeEditor target) {
        idle.stop();
        target.caretPositionProperty().removeListener(caretListener);
        if (stopListening != null) {
            stopListening.run();
            stopListening = null;
        }
        clear(target);
        editor = null;
    }

    private void onTextChanged(int position, int removed, int inserted) {
        for (int i = 0; i < marks.size(); i++) {
            int offset = marks.get(i);
            if (offset >= position + removed) {
                marks.set(i, offset + inserted - removed);
            } else if (offset >= position) {
                marks.set(i, -1);
            }
        }
        idle.playFromStart();
        schedule();
    }

    private void schedule() {
        if (pending) {
            return;
        }
        pending = true;
        Platform.runLater(() -> {
            pending = false;
            update();
        });
    }

    private void update() {
        CodeEditor target = editor;
        if (target == null) {
            return;
        }
        clear(target);
        if (target.getSelection().getLength() != 0) {
            return;
        }
        int caret = target.getCaretPosition();
        int from = Math.max(0, caret - WINDOW);
        int to = Math.min(target.getLength(), caret + WINDOW);
        String text = target.getText(from, to);
        int[] pair = BracketLogic.match(text, caret - from, offset -> isIgnored(target, from + offset));
        if (pair == null) {
            return;
        }
        mark(target, from + pair[0]);
        mark(target, from + pair[1]);
    }

    private boolean isIgnored(CodeEditor target, int offset) {
        Collection<String> classes = target.getStyleClassesAt(offset);
        return classes.contains("tok-string") || classes.contains("tok-comment");
    }

    private void mark(CodeEditor target, int offset) {
        Collection<String> existing = target.getStyleClassesAt(offset);
        if (!existing.contains(MARK_CLASS)) {
            List<String> updated = new ArrayList<>(existing);
            updated.add(MARK_CLASS);
            target.setStyleClasses(offset, offset + 1, updated);
        }
        marks.add(offset);
    }

    private void clear(CodeEditor target) {
        for (int offset : marks) {
            if (offset < 0 || offset >= target.getLength()) {
                continue;
            }
            Collection<String> existing = target.getStyleClassesAt(offset);
            if (existing.contains(MARK_CLASS)) {
                List<String> updated = new ArrayList<>(existing);
                updated.remove(MARK_CLASS);
                target.setStyleClasses(offset, offset + 1, updated);
            }
        }
        marks.clear();
    }
}