package io.github.itech_framework.code_editor.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.itech_framework.code_editor.CodeEditor;
import io.github.itech_framework.code_editor.EditorFeature;
import io.github.itech_framework.code_editor.TextEdit;
import io.github.itech_framework.code_editor.formatting.CodeFormatter;
import io.github.itech_framework.code_editor.formatting.FormatOptions;
import io.github.itech_framework.code_editor.internal.BuiltInFormatters;
import io.github.itech_framework.code_editor.internal.FormatLogic;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public final class FormatFeature implements EditorFeature {

    public static final String ID = "format";

    private static final System.Logger LOG = System.getLogger(FormatFeature.class.getName());

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "code-editor-formatter");
        thread.setDaemon(true);
        return thread;
    });

    private final EventHandler<KeyEvent> keyHandler = this::onKeyPressed;

    private final AtomicInteger generation = new AtomicInteger();

    private CodeEditor editor;

    private Runnable cancelListener;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void install(CodeEditor target) {
        editor = target;
        cancelListener = target.addTextChangeListener((position, removed, inserted) -> generation.incrementAndGet());
        target.addEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
    }

    @Override
    public void uninstall(CodeEditor target) {
        target.removeEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
        if (cancelListener != null) {
            cancelListener.run();
            cancelListener = null;
        }
        editor = null;
    }

    private void onKeyPressed(KeyEvent event) {
        CodeEditor target = editor;
        if (target == null || !target.isTextAreaEvent(event)) {
            return;
        }
        if (event.getCode() != KeyCode.F || !event.isShiftDown() || !event.isAltDown() || event.isControlDown()
                || event.isMetaDown()) {
            return;
        }
        event.consume();
        if (!target.isEditable()) {
            return;
        }
        CodeFormatter formatter = target.getFormatter();
        if (formatter == null) {
            formatter = BuiltInFormatters.forLanguage(target.getLanguageDefinition());
        }
        if (formatter == null) {
            return;
        }
        CodeFormatter chosen = formatter;
        String text = target.getText();
        int ticket = generation.get();
        FormatOptions options = new FormatOptions(target.getTabSize(), target.isInsertSpaces());
        WORKER.execute(() -> {
            List<TextEdit> edits;
            try {
                edits = chosen.format(text, options);
            } catch (RuntimeException ex) {
                LOG.log(System.Logger.Level.WARNING, "Formatting failed", ex);
                return;
            }
            if (edits == null || edits.isEmpty()) {
                return;
            }
            Platform.runLater(() -> apply(text, ticket, edits));
        });
    }

    private void apply(String oldText, int ticket, List<TextEdit> edits) {
        CodeEditor target = editor;
        if (target == null || generation.get() != ticket) {
            return;
        }
        StringBuilder builder = new StringBuilder(oldText.length());
        int position = 0;
        for (TextEdit edit : edits) {
            builder.append(oldText, position, edit.start()).append(edit.text());
            position = edit.end();
        }
        builder.append(oldText, position, oldText.length());
        String newText = builder.toString();
        List<IndexRange> ranges = target.getSelectionRanges();
        try {
            target.applyEdits(edits);
        } catch (IllegalArgumentException ex) {
            LOG.log(System.Logger.Level.WARNING, "Formatter returned invalid edits", ex);
            return;
        }
        List<IndexRange> mapped = new ArrayList<>(ranges.size());
        for (IndexRange range : ranges) {
            int start = FormatLogic.mapOffset(oldText, newText, range.getStart());
            int end = FormatLogic.mapOffset(oldText, newText, range.getEnd());
            mapped.add(new IndexRange(Math.min(start, end), Math.max(start, end)));
        }
        target.setSelectionRanges(mapped.get(0), mapped.subList(1, mapped.size()));
        target.revealSelection();
    }
}