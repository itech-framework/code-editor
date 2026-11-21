package io.github.itech_framework.code_editor.internal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.fxmisc.richtext.model.TwoDimensional.Bias;

import io.github.itech_framework.code_editor.language.LineTokens;
import io.github.itech_framework.code_editor.language.Token;
import io.github.itech_framework.code_editor.language.Tokenizer;
import io.github.itech_framework.code_editor.language.TokenizerState;
import javafx.application.Platform;

public final class SyntaxHighlighter {

    private static final System.Logger LOG = System.getLogger(SyntaxHighlighter.class.getName());

    private static final int MAX_LINE_LENGTH = 20_000;

    private static final Duration IDLE_DELAY = Duration.ofMillis(60);

    private static final List<String> NO_STYLE = List.of();

    private static final Map<String, List<String>> STYLES = new ConcurrentHashMap<>();

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "code-editor-highlighter");
        thread.setDaemon(true);
        return thread;
    });

    private record LineInfo(TokenizerState start, TokenizerState end) {
    }

    private record Outcome(int token, Tokenizer tokenizer, List<LineInfo> lines, int from,
            StyleSpans<Collection<String>> spans) {
    }

    private final CodeArea area;

    private final AtomicInteger latest = new AtomicInteger();
    
    private final List<Runnable> appliedListeners = new CopyOnWriteArrayList<>();

    private Tokenizer tokenizer;

    private List<LineInfo> cache = new ArrayList<>();

    public SyntaxHighlighter(CodeArea area, Tokenizer tokenizer) {
        this.area = Objects.requireNonNull(area, "area");
        this.tokenizer = Objects.requireNonNull(tokenizer, "tokenizer");
        resetCache();
        area.multiPlainChanges().subscribe(this::onChanges);
        area.plainTextChanges().successionEnds(IDLE_DELAY).subscribe(change -> schedule());
    }
    
    public Runnable addAppliedListener(Runnable listener) {
        appliedListeners.add(listener);
        return () -> appliedListeners.remove(listener);
    }

    public void setTokenizer(Tokenizer tokenizer) {
        this.tokenizer = Objects.requireNonNull(tokenizer, "tokenizer");
        latest.incrementAndGet();
        resetCache();
        schedule();
    }

    private void onChanges(List<PlainTextChange> changes) {
        if (changes.size() == 1) {
            onChange(changes.get(0));
            return;
        }
        latest.incrementAndGet();
        resetCache();
    }
    
    private void onChange(PlainTextChange change) {
        latest.incrementAndGet();
        int line = area.offsetToPosition(change.getPosition(), Bias.Forward).getMajor();
        if (line >= cache.size()) {
            resetCache();
            return;
        }
        int removed = breaks(change.getRemoved());
        int inserted = breaks(change.getInserted());
        cache.set(line, null);
        if (removed > 0) {
            int end = Math.min(cache.size(), line + 1 + removed);
            cache.subList(line + 1, end).clear();
        }
        if (inserted > 0) {
            cache.addAll(line + 1, Collections.nCopies(inserted, (LineInfo) null));
        }
    }

    private void schedule() {
        int count = area.getParagraphs().size();
        if (cache.size() != count) {
            resetCache();
        }
        List<String> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lines.add(area.getText(i));
        }
        List<LineInfo> snapshot = new ArrayList<>(cache);
        int token = latest.get();
        Tokenizer current = tokenizer;
        WORKER.execute(() -> {
            try {
                run(token, current, lines, snapshot);
            } catch (RuntimeException ex) {
                LOG.log(System.Logger.Level.WARNING, "Syntax highlighting failed", ex);
            }
        });
    }

    private void run(int token, Tokenizer tk, List<String> lines, List<LineInfo> old) {
        int count = lines.size();
        int first = 0;
        while (first < count && old.get(first) != null) {
            first++;
        }
        if (first == count) {
            return;
        }
        int lastDirty = count - 1;
        while (old.get(lastDirty) != null) {
            lastDirty--;
        }
        int from = 0;
        for (int i = 0; i < first; i++) {
            from += lines.get(i).length() + 1;
        }
        TokenizerState state = first == 0 ? tk.initialState() : old.get(first - 1).end();
        List<LineInfo> updated = new ArrayList<>(old);
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        int covered = 0;
        for (int i = first; i < count; i++) {
            if (latest.get() != token) {
                return;
            }
            LineInfo previous = old.get(i);
            if (i > lastDirty && previous != null && previous.start().equals(state)) {
                break;
            }
            String line = lines.get(i);
            LineTokens result = line.length() > MAX_LINE_LENGTH ? new LineTokens(List.of(), state)
                    : tk.tokenize(line, state);
            updated.set(i, new LineInfo(state, result.endState()));
            appendSpans(builder, line, result.tokens());
            covered += line.length();
            if (i < count - 1) {
                builder.add(NO_STYLE, 1);
                covered++;
            }
            state = result.endState();
        }
        StyleSpans<Collection<String>> spans = covered > 0 ? builder.create() : null;
        Outcome outcome = new Outcome(token, tk, updated, from, spans);
        Platform.runLater(() -> apply(outcome));
    }

    private void apply(Outcome outcome) {
        if (outcome.token() != latest.get() || outcome.tokenizer() != tokenizer) {
            return;
        }
        cache = outcome.lines();
        if (outcome.spans() != null) {
            area.setStyleSpans(outcome.from(), outcome.spans());
            for (Runnable listener : appliedListeners) {
                listener.run();
            }
        }
    }

    private void resetCache() {
        cache = new ArrayList<>(Collections.nCopies(area.getParagraphs().size(), (LineInfo) null));
    }

    private static void appendSpans(StyleSpansBuilder<Collection<String>> builder, String line, List<Token> tokens) {
        int position = 0;
        for (Token token : tokens) {
            int start = Math.max(token.start(), position);
            int end = Math.min(token.end(), line.length());
            if (end <= start) {
                continue;
            }
            if (start > position) {
                builder.add(NO_STYLE, start - position);
            }
            builder.add(stylesFor(token.scope()), end - start);
            position = end;
        }
        if (position < line.length()) {
            builder.add(NO_STYLE, line.length() - position);
        }
    }

    private static List<String> stylesFor(String scope) {
        return STYLES.computeIfAbsent(scope, value -> {
            List<String> classes = new ArrayList<>();
            StringBuilder name = new StringBuilder("tok");
            for (String part : value.split("\\.")) {
                if (part.isEmpty()) {
                    continue;
                }
                name.append('-').append(part.replaceAll("[^A-Za-z0-9_]", "_"));
                classes.add(name.toString());
            }
            return List.copyOf(classes);
        });
    }

    private static int breaks(String text) {
        int total = 0;
        for (int i = text.indexOf('\n'); i >= 0; i = text.indexOf('\n', i + 1)) {
            total++;
        }
        return total;
    }
}