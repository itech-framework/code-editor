package io.github.itech_framework.code_editor.internal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
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

import io.github.itech_framework.code_editor.language.Token;
import io.github.itech_framework.code_editor.language.Tokenizer;
import io.github.itech_framework.code_editor.language.TokenizerState;
import javafx.application.Platform;

public final class SyntaxHighlighter {

    private static final System.Logger LOG = System.getLogger(SyntaxHighlighter.class.getName());

    private static final int MAX_LINE_LENGTH = 20_000;

    private static final int WINDOW_LINES = 500;

    private static final Duration IDLE_DELAY = Duration.ofMillis(60);

    private static final List<String> NO_STYLE = List.of();

    private static final Map<String, List<String>> STYLES = new ConcurrentHashMap<>();

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "code-editor-highlighter");
        thread.setDaemon(true);
        return thread;
    });

    private record Outcome(int token, Tokenizer tokenizer, int first, List<HighlightRun.LineInfo> infos, int from,
            StyleSpans<Collection<String>> spans, boolean incomplete) {
    }

    private final CodeArea area;

    private final AtomicInteger latest = new AtomicInteger();

    private final List<Runnable> appliedListeners = new CopyOnWriteArrayList<>();

    private final HighlightCache cache = new HighlightCache();

    private Tokenizer tokenizer;

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
        } else {
            latest.incrementAndGet();
            resetCache();
        }
    }

    private void onChange(PlainTextChange change) {
        latest.incrementAndGet();
        int line = area.offsetToPosition(change.getPosition(), Bias.Forward).getMajor();
        if (line >= cache.size()) {
            resetCache();
            return;
        }
        cache.edit(line, breaks(change.getRemoved()), breaks(change.getInserted()));
    }

    private void schedule() {
        int count = area.getParagraphs().size();
        if (cache.size() != count) {
            resetCache();
        }
        HighlightCache.Plan plan = cache.plan(WINDOW_LINES);
        if (plan == null) {
            return;
        }
        List<String> lines = new ArrayList<>(plan.end() - plan.first());
        for (int i = plan.first(); i < plan.end(); i++) {
            lines.add(area.getText(i));
        }
        int from = area.getAbsolutePosition(plan.first(), 0);
        int token = latest.get();
        Tokenizer current = tokenizer;
        WORKER.execute(() -> {
            try {
                run(token, current, plan, lines, from, count);
            } catch (RuntimeException ex) {
                LOG.log(System.Logger.Level.WARNING, "Syntax highlighting failed", ex);
            }
        });
    }

    private void run(int token, Tokenizer tk, HighlightCache.Plan plan, List<String> lines, int from, int count) {
        TokenizerState start = plan.previous() == null ? tk.initialState() : plan.previous().end();
        HighlightRun.Result result = HighlightRun.run(tk, lines, start, plan.window(), plan.lastDirty(), plan.more(),
                MAX_LINE_LENGTH, () -> latest.get() != token);
        if (result == null) {
            return;
        }
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        int covered = 0;
        for (int k = 0; k < result.infos().size(); k++) {
            String line = lines.get(k);
            appendSpans(builder, line, result.tokens().get(k));
            covered += line.length();
            if (plan.first() + k < count - 1) {
                builder.add(NO_STYLE, 1);
                covered++;
            }
        }
        StyleSpans<Collection<String>> spans = covered > 0 ? builder.create() : null;
        Outcome outcome = new Outcome(token, tk, plan.first(), result.infos(), from, spans, result.incomplete());
        Platform.runLater(() -> apply(outcome));
    }

    private void apply(Outcome outcome) {
        if (outcome.token() != latest.get() || outcome.tokenizer() != tokenizer) {
            return;
        }
        cache.apply(outcome.first(), outcome.infos(), outcome.incomplete());
        if (outcome.spans() != null) {
            area.setStyleSpans(outcome.from(), outcome.spans());
            for (Runnable listener : appliedListeners) {
                listener.run();
            }
        }
        if (outcome.incomplete()) {
            schedule();
        }
    }

    private void resetCache() {
        cache.reset(area.getParagraphs().size());
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