package io.github.itech_framework.code_editor.demo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import io.github.itech_framework.code_editor.CodeEditor;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;

public final class PerfBench {

    private static final int[] SIZES = {100_000, 1_000_000, 5_000_000};
    private static final int KEYSTROKES = 200;
    private static final int KEY_INTERVAL_MS = 80;
    private static final int GETTEXT_ROUNDS = 20;
    private static final int TAIL_MS = 1500;
    private static final long QUIET_MS = 600;
    private static final long SETTLE_TIMEOUT_MS = 60_000;
    private static final double FRAME_MS = 16.7;
    private static final double SLOW_FRAME_MS = 25;

    private record Frames(double worstMs, double stalledMs) {
    }

    private record Typing(double avgMs, double worstMs, Frames frames) {
    }

    private record Result(String label, int chars, double loadMs, double settleMs, boolean settleTimedOut,
            Frames loadFrames, double missMs, double hitMs, double enableMs, Frames enableFrames, Typing base,
            Typing full) {
    }

    private static final class FrameMonitor extends AnimationTimer {

        private long last;
        private double worst;
        private double stalled;

        @Override
        public void handle(long now) {
            if (last != 0) {
                double gap = (now - last) / 1_000_000.0;
                worst = Math.max(worst, gap);
                if (gap > SLOW_FRAME_MS) {
                    stalled += gap - FRAME_MS;
                }
            }
            last = now;
        }

        Frames result() {
            return new Frames(worst, stalled);
        }
    }

    private final CodeEditor editor;
    private final Consumer<String> status;
    private final AtomicLong lastHighlight = new AtomicLong();
    private final AtomicInteger highlights = new AtomicInteger();
    private volatile boolean running;
    private long sink;

    public PerfBench(CodeEditor editor, Consumer<String> status) {
        this.editor = editor;
        this.status = status;
    }

    public boolean isRunning() {
        return running;
    }

    public void start(Runnable onFinished) {
        if (running) {
            return;
        }
        running = true;
        Thread worker = new Thread(() -> {
            try {
                run();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                ex.printStackTrace();
                show("Benchmark failed: " + ex);
            } finally {
                running = false;
                Platform.runLater(onFinished);
            }
        }, "perf-bench");
        worker.setDaemon(true);
        worker.start();
    }

    private void run() throws Exception {
        String originalText = fx(editor::getText);
        String originalLanguage = fx(editor::getLanguage);
        boolean originalAutoOpen = fx(editor::isCompletionAutoOpen);
        Map<String, Boolean> originalFeatures = fx(() -> {
            Map<String, Boolean> states = new LinkedHashMap<>();
            for (String id : editor.features().ids()) {
                states.put(id, editor.features().isEnabled(id));
            }
            return states;
        });
        Runnable cancel = fx(() -> editor.addHighlightListener(() -> {
            lastHighlight.set(System.nanoTime());
            highlights.incrementAndGet();
        }));
        List<Result> results = new ArrayList<>();
        try {
            onFx(() -> {
                editor.setLanguage("javascript");
                editor.requestFocus();
            });
            for (int size : SIZES) {
                Result result = measure(size, originalFeatures);
                results.add(result);
                print(result);
            }
        } finally {
            onFx(() -> {
                cancel.run();
                applyFeatures(originalFeatures);
                editor.setCompletionAutoOpen(originalAutoOpen);
                editor.setLanguage(originalLanguage);
                editor.setText(originalText);
            });
        }
        printSummary(results);
        show("Benchmark finished. Copy the console output.");
    }

    private Result measure(int size, Map<String, Boolean> features) throws Exception {
        String label = size >= 1_000_000 ? size / 1_000_000 + " MB" : size / 1_000 + " KB";
        show(label + ": generating text");
        String text = generate(size);

        Map<String, Boolean> off = new LinkedHashMap<>();
        features.keySet().forEach(id -> off.put(id, false));
        onFx(() -> applyFeatures(off));

        show(label + ": loading text");
        FrameMonitor loadMonitor = startFrames();
        int before = highlights.get();
        long[] startNanos = new long[1];
        double[] loadMs = new double[1];
        fx(() -> {
            lastHighlight.set(System.nanoTime());
            startNanos[0] = System.nanoTime();
            editor.setText(text);
            loadMs[0] = (System.nanoTime() - startNanos[0]) / 1_000_000.0;
            return null;
        });
        boolean[] timedOut = new boolean[1];
        double settleMs = awaitHighlight(before, startNanos[0], timedOut);
        Frames loadFrames = stopFrames(loadMonitor);

        show(label + ": getText after edit");
        double[] getText = fx(() -> {
            int middle = editor.getLength() / 2;
            double miss = 0;
            double hit = 0;
            for (int i = 0; i < GETTEXT_ROUNDS; i++) {
                editor.replaceText(middle, middle, "x");
                long a = System.nanoTime();
                int first = editor.getText().length();
                long b = System.nanoTime();
                int second = editor.getText().length();
                long c = System.nanoTime();
                sink += first + second;
                miss += b - a;
                hit += c - b;
            }
            return new double[] {miss / GETTEXT_ROUNDS / 1_000_000.0, hit / GETTEXT_ROUNDS / 1_000_000.0};
        });
        awaitHighlight(highlights.get(), System.nanoTime(), new boolean[1]);

        Typing base = typeKeys(label + ": typing, highlighter only");

        show(label + ": enabling all features");
        FrameMonitor enableMonitor = startFrames();
        double[] enableMs = new double[1];
        fx(() -> {
            long t0 = System.nanoTime();
            applyFeatures(features);
            editor.setCompletionAutoOpen(true);
            enableMs[0] = (System.nanoTime() - t0) / 1_000_000.0;
            return null;
        });
        Thread.sleep(2000 + size / 500);
        Frames enableFrames = stopFrames(enableMonitor);

        Typing full = typeKeys(label + ": typing, all features");

        return new Result(label, text.length(), loadMs[0], settleMs, timedOut[0], loadFrames, getText[0],
                getText[1], enableMs[0], enableFrames, base, full);
    }

    private Typing typeKeys(String message) throws Exception {
        show(message);
        int line = fx(() -> editor.getLineCount() / 2);
        int position = fx(() -> editor.getLineStart(line));
        FrameMonitor monitor = startFrames();
        double total = 0;
        double worst = 0;
        for (int i = 0; i < KEYSTROKES; i++) {
            int at = position + i;
            String key = String.valueOf((char) ('a' + i % 26));
            long nanos = fx(() -> {
                long t0 = System.nanoTime();
                editor.moveCaret(at);
                editor.replaceText(at, at, key);
                editor.moveCaret(at + 1);
                return System.nanoTime() - t0;
            });
            total += nanos;
            worst = Math.max(worst, nanos);
            Thread.sleep(KEY_INTERVAL_MS);
        }
        Thread.sleep(TAIL_MS);
        Frames frames = stopFrames(monitor);
        return new Typing(total / KEYSTROKES / 1_000_000.0, worst / 1_000_000.0, frames);
    }

    private double awaitHighlight(int eventsBefore, long fromNanos, boolean[] timedOut) throws InterruptedException {
        while (true) {
            Thread.sleep(100);
            long now = System.nanoTime();
            long elapsed = (now - fromNanos) / 1_000_000;
            long quiet = (now - lastHighlight.get()) / 1_000_000;
            if (highlights.get() > eventsBefore && quiet >= QUIET_MS) {
                return elapsed;
            }
            if (elapsed >= SETTLE_TIMEOUT_MS) {
                timedOut[0] = true;
                return elapsed;
            }
        }
    }

    private void applyFeatures(Map<String, Boolean> states) {
        for (Map.Entry<String, Boolean> entry : states.entrySet()) {
            editor.features().setEnabled(entry.getKey(), entry.getValue());
        }
    }

    private FrameMonitor startFrames() throws Exception {
        return fx(() -> {
            FrameMonitor monitor = new FrameMonitor();
            monitor.start();
            return monitor;
        });
    }

    private Frames stopFrames(FrameMonitor monitor) throws Exception {
        return fx(() -> {
            monitor.stop();
            return monitor.result();
        });
    }

    private <T> T fx(Callable<T> task) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return task.call();
        }
        FutureTask<T> future = new FutureTask<>(task);
        Platform.runLater(future);
        try {
            return future.get();
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw ex;
        }
    }

    private void onFx(Runnable task) throws Exception {
        fx(() -> {
            task.run();
            return null;
        });
    }

    private void show(String message) {
        System.out.println("[bench] " + message);
        status.accept(message);
    }

    private static String f(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String f(double a, double b) {
        return f(a) + " / " + f(b);
    }

    private void print(Result r) {
        System.out.println();
        System.out.println("== " + r.label() + " (" + r.chars() + " chars)");
        System.out.println("load:        setText " + f(r.loadMs()) + " ms | highlight settled after " + f(r.settleMs())
                + " ms" + (r.settleTimedOut() ? " (TIMED OUT)" : "") + " | worst frame " + f(r.loadFrames().worstMs())
                + " ms | stalled " + f(r.loadFrames().stalledMs()) + " ms");
        System.out.println("getText:     first after edit " + f(r.missMs()) + " ms | second call " + f(r.hitMs()) + " ms");
        System.out.println("features on: enable " + f(r.enableMs()) + " ms | worst frame "
                + f(r.enableFrames().worstMs()) + " ms | stalled " + f(r.enableFrames().stalledMs()) + " ms");
        System.out.println("typing " + KEYSTROKES + " keys, " + KEY_INTERVAL_MS + " ms apart (highlighter only / all features)");
        System.out.println("  insert avg ms:      " + f(r.base().avgMs(), r.full().avgMs()));
        System.out.println("  insert worst ms:    " + f(r.base().worstMs(), r.full().worstMs()));
        System.out.println("  worst frame ms:     " + f(r.base().frames().worstMs(), r.full().frames().worstMs()));
        System.out.println("  stalled total ms:   " + f(r.base().frames().stalledMs(), r.full().frames().stalledMs()));
        status.accept(r.label() + ": getText " + f(r.missMs()) + "/" + f(r.hitMs()) + " ms, insert avg "
                + f(r.base().avgMs(), r.full().avgMs()) + " ms, worst frame "
                + f(r.base().frames().worstMs(), r.full().frames().worstMs()) + " ms");
    }

    private void printSummary(List<Result> results) {
        System.out.println();
        System.out.println("SUMMARY (ms; pairs are highlighter only / all features)");
        System.out.println("size | load | getText first/second | insert avg | insert worst | worst frame | stalled");
        for (Result r : results) {
            System.out.println(r.label() + " | " + f(r.loadMs()) + " | " + f(r.missMs(), r.hitMs()) + " | "
                    + f(r.base().avgMs(), r.full().avgMs()) + " | " + f(r.base().worstMs(), r.full().worstMs()) + " | "
                    + f(r.base().frames().worstMs(), r.full().frames().worstMs()) + " | "
                    + f(r.base().frames().stalledMs(), r.full().frames().stalledMs()));
        }
        System.out.println("(checksum " + sink + ")");
    }

    private static String generate(int chars) {
        StringBuilder sb = new StringBuilder(chars + 512);
        int n = 0;
        while (sb.length() < chars) {
            sb.append("// block ").append(n).append('\n');
            sb.append("function compute").append(n).append("(items, options) {\n");
            sb.append("  const result = [];\n");
            sb.append("  for (let i = 0; i < items.length; i++) {\n");
            sb.append("    if (items[i].value > ").append(n % 97).append(" && options.enabled) {\n");
            sb.append("      result.push({ id: items[i].id, label: \"item-\" + i + \" မင်္ဂလာပါ\", total: items[i].value * 2.5 });\n");
            sb.append("    } else {\n");
            sb.append("      /* skipped ").append(n).append(" */\n");
            sb.append("    }\n");
            sb.append("  }\n");
            sb.append("  return result;\n");
            sb.append("}\n\n");
            n++;
        }
        return sb.toString();
    }
}