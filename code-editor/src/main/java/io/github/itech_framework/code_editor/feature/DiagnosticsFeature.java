package io.github.itech_framework.code_editor.feature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.itech_framework.code_editor.CodeEditor;
import io.github.itech_framework.code_editor.EditorFeature;
import io.github.itech_framework.code_editor.diagnostics.Diagnostic;
import io.github.itech_framework.code_editor.diagnostics.DiagnosticSeverity;
import io.github.itech_framework.code_editor.diagnostics.DiagnosticsProvider;
import io.github.itech_framework.code_editor.internal.BuiltInDiagnostics;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.util.Duration;

public final class DiagnosticsFeature implements EditorFeature {

    public static final String ID = "diagnostics";

    public static final int MAX_DIAGNOSTICS = 200;

    private static final System.Logger LOG = System.getLogger(DiagnosticsFeature.class.getName());

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "code-editor-diagnostics");
        thread.setDaemon(true);
        return thread;
    });

    private final PauseTransition idle = new PauseTransition(Duration.millis(400));

    private final ChangeListener<Object> restartListener = (observable, old, value) -> restart();

    private volatile int generation;

    private List<Diagnostic> current = List.of();

    private CodeEditor editor;

    private Runnable stopTextListening;

    private Runnable stopHighlightListening;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void install(CodeEditor target) {
        editor = target;
        idle.setOnFinished(event -> run());
        stopTextListening = target.addTextChangeListener(this::onTextChanged);
        stopHighlightListening = target.addHighlightListener(this::reapply);
        target.languageProperty().addListener(restartListener);
        target.diagnosticsProviderProperty().addListener(restartListener);
        idle.playFromStart();
    }

    @Override
    public void uninstall(CodeEditor target) {
        idle.stop();
        idle.setOnFinished(null);
        generation++;
        target.languageProperty().removeListener(restartListener);
        target.diagnosticsProviderProperty().removeListener(restartListener);
        if (stopTextListening != null) {
            stopTextListening.run();
            stopTextListening = null;
        }
        if (stopHighlightListening != null) {
            stopHighlightListening.run();
            stopHighlightListening = null;
        }
        clear();
        editor = null;
    }

    private void restart() {
        generation++;
        idle.playFromStart();
    }

    private void onTextChanged(int position, int removed, int inserted) {
        generation++;
        idle.playFromStart();
        if (current.isEmpty()) {
            return;
        }
        int editEnd = position + removed;
        int delta = inserted - removed;
        List<Diagnostic> shifted = new ArrayList<>(current.size());
        for (Diagnostic diagnostic : current) {
            int start = map(diagnostic.start(), position, editEnd, delta, position);
            int end = map(diagnostic.end(), position, editEnd, delta, position + inserted);
            shifted.add(new Diagnostic(start, end, diagnostic.severity(), diagnostic.message()));
        }
        current = List.copyOf(shifted);
        editor.publishDiagnostics(current);
    }

    private static int map(int offset, int position, int editEnd, int delta, int insideRemoved) {
        if (offset <= position) {
            return offset;
        }
        if (offset >= editEnd) {
            return offset + delta;
        }
        return insideRemoved;
    }

    private void run() {
        if (editor == null) {
            return;
        }
        DiagnosticsProvider provider = editor.getDiagnosticsProvider();
        if (provider == null) {
            provider = BuiltInDiagnostics.forLanguage(editor.getLanguage());
        }
        if (provider == null) {
            clear();
            return;
        }
        DiagnosticsProvider chosen = provider;
        String text = editor.getText();
        int ticket = generation;
        WORKER.execute(() -> {
            if (ticket != generation) {
                return;
            }
            List<Diagnostic> found;
            try {
                found = chosen.diagnose(text);
            } catch (RuntimeException ex) {
                LOG.log(System.Logger.Level.WARNING, "Diagnostics provider failed", ex);
                found = List.of();
            }
            List<Diagnostic> delivered = found == null ? List.of() : found;
            Platform.runLater(() -> accept(ticket, delivered));
        });
    }

    private void accept(int ticket, List<Diagnostic> found) {
        if (editor == null || ticket != generation) {
            return;
        }
        List<Diagnostic> sanitized = sanitize(found, editor.getLength());
        removeMarks();
        current = sanitized;
        editor.publishDiagnostics(sanitized);
        applyMarks();
    }

    private void clear() {
        removeMarks();
        current = List.of();
        if (editor != null) {
            editor.publishDiagnostics(current);
        }
    }

    private void reapply() {
        if (!current.isEmpty()) {
            applyMarks();
        }
    }

    private void applyMarks() {
        int length = editor.getLength();
        for (Diagnostic diagnostic : current) {
            int[] range = range(diagnostic, length);
            if (range != null) {
                editor.addStyleClass(range[0], range[1], diagnostic.severity().styleClass());
            }
        }
    }

    private void removeMarks() {
        if (editor == null || current.isEmpty()) {
            return;
        }
        int length = editor.getLength();
        for (DiagnosticSeverity severity : DiagnosticSeverity.values()) {
            editor.removeStyleClass(0, length, severity.styleClass());
        }
    }

    private static int[] range(Diagnostic diagnostic, int length) {
        int start = Math.min(diagnostic.start(), length);
        int end = Math.min(diagnostic.end(), length);
        if (end > start) {
            return new int[] { start, end };
        }
        if (length == 0) {
            return null;
        }
        if (end < length) {
            return new int[] { start, end + 1 };
        }
        return new int[] { length - 1, length };
    }

    private static List<Diagnostic> sanitize(List<Diagnostic> found, int length) {
        List<Diagnostic> result = new ArrayList<>();
        for (Diagnostic diagnostic : found) {
            if (diagnostic == null || diagnostic.start() > length) {
                continue;
            }
            result.add(diagnostic.end() > length
                    ? new Diagnostic(diagnostic.start(), length, diagnostic.severity(), diagnostic.message())
                    : diagnostic);
        }
        if (result.size() > MAX_DIAGNOSTICS) {
            result.sort(Comparator.comparing(Diagnostic::severity).thenComparingInt(Diagnostic::start));
            result = new ArrayList<>(result.subList(0, MAX_DIAGNOSTICS));
        }
        result.sort(Comparator.comparingInt(Diagnostic::start).thenComparing(Diagnostic::severity));
        return List.copyOf(result);
    }
}