package io.github.itech_framework.code_editor.feature;

import java.util.BitSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.itech_framework.code_editor.CodeEditor;
import io.github.itech_framework.code_editor.EditorFeature;
import io.github.itech_framework.code_editor.internal.FoldLogic;
import io.github.itech_framework.code_editor.language.LanguageRegistry;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public final class FoldFeature implements EditorFeature {

    public static final String ID = "fold";

    private static final Set<String> SKIPPED = Set.of("tok-string", "tok-comment");

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "code-editor-fold");
        thread.setDaemon(true);
        return thread;
    });

    private final EventHandler<KeyEvent> keyHandler = this::onKeyPressed;

    private final ChangeListener<String> languageListener = (obs, old, value) -> request();

    private CodeEditor editor;

    private Runnable stopTextListening;

    private Runnable stopHighlightListening;

    private int generation;

    private boolean hasFolds;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void install(CodeEditor target) {
        editor = target;
        target.addEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
        target.languageProperty().addListener(languageListener);
        stopTextListening = target.addTextChangeListener((position, removed, inserted) -> generation++);
        stopHighlightListening = target.addHighlightListener(this::request);
        Platform.runLater(this::request);
    }

    @Override
    public void uninstall(CodeEditor target) {
        generation++;
        target.removeEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
        target.languageProperty().removeListener(languageListener);
        if (stopTextListening != null) {
            stopTextListening.run();
            stopTextListening = null;
        }
        if (stopHighlightListening != null) {
            stopHighlightListening.run();
            stopHighlightListening = null;
        }
        apply(target, List.of());
        editor = null;
    }

    public boolean foldAtCaret() {
        CodeEditor target = editor;
        if (target == null) {
            return false;
        }
        int line = target.getLineOfOffset(target.getCaretPosition());
        FoldLogic.Range range = FoldLogic.innermost(target.foldState().getRanges(), line, r -> canFold(target, r));
        if (range == null) {
            return false;
        }
        target.hideLines(range.firstHiddenLine(), range.lastHiddenLine());
        hasFolds = true;
        target.foldState().touch();
        return true;
    }

    public boolean unfoldAtCaret() {
        CodeEditor target = editor;
        if (target == null) {
            return false;
        }
        int line = target.getLineOfOffset(target.getCaretPosition());
        FoldLogic.Range range = FoldLogic.innermost(target.foldState().getRanges(), line, r -> canUnfold(target, r));
        if (range == null) {
            return false;
        }
        target.showLinesAfter(range.startLine());
        target.foldState().touch();
        return true;
    }

    private void onKeyPressed(KeyEvent event) {
        CodeEditor target = editor;
        if (target == null || !target.isTextAreaEvent(event)) {
            return;
        }
        if (!event.isShortcutDown() || !event.isShiftDown() || event.isAltDown()) {
            return;
        }
        KeyCode code = event.getCode();
        if (code == KeyCode.OPEN_BRACKET) {
            event.consume();
            foldAtCaret();
        } else if (code == KeyCode.CLOSE_BRACKET) {
            event.consume();
            unfoldAtCaret();
        }
    }

    private void request() {
        CodeEditor target = editor;
        if (target == null) {
            return;
        }
        int token = ++generation;
        if (LanguageRegistry.PLAIN_TEXT.equals(target.getLanguage())) {
            apply(target, List.of());
            return;
        }
        String text = target.getText();
        BitSet skipped = target.offsetsWithStyle(SKIPPED);
        WORKER.execute(() -> {
            List<FoldLogic.Range> ranges = FoldLogic.compute(text, skipped::get);
            Platform.runLater(() -> {
                if (editor == target && generation == token) {
                    apply(target, ranges);
                }
            });
        });
    }

    private void apply(CodeEditor target, List<FoldLogic.Range> ranges) {
        if (hasFolds) {
            FoldLogic.Scan scan = FoldLogic.scan(ranges, target::isLineHidden, target.getLineCount());
            for (int header : scan.stale()) {
                target.showLinesAfter(header);
            }
            hasFolds = scan.runs() > scan.stale().size();
        }
        target.foldState().setRanges(ranges);
    }

    private static boolean valid(CodeEditor target, FoldLogic.Range range) {
        return range.endLine() < target.getLineCount();
    }

    private static boolean canFold(CodeEditor target, FoldLogic.Range range) {
        return valid(target, range) && !target.isLineHidden(range.firstHiddenLine());
    }

    private static boolean canUnfold(CodeEditor target, FoldLogic.Range range) {
        return valid(target, range) && target.isLineHidden(range.firstHiddenLine())
                && !target.isLineHidden(range.startLine());
    }
}