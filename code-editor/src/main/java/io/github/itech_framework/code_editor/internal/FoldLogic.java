package io.github.itech_framework.code_editor.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

public final class FoldLogic {

    public static final int MAX_DEPTH = 256;

    private static final String OPENING = "{[";
    private static final String CLOSING = "}]";

    private FoldLogic() {
    }

    public record Range(int startLine, int endLine) {

        public int firstHiddenLine() {
            return startLine + 1;
        }

        public int lastHiddenLine() {
            return endLine - 1;
        }
    }

    public record Scan(List<Integer> stale, int runs) {
    }

    public static List<Range> compute(CharSequence text, IntPredicate ignored) {
        Map<Integer, Integer> byStart = new TreeMap<>();
        int[] kinds = new int[MAX_DEPTH];
        int[] lines = new int[MAX_DEPTH];
        int depth = 0;
        int line = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                line++;
                continue;
            }
            int open = OPENING.indexOf(c);
            int close = CLOSING.indexOf(c);
            if ((open < 0 && close < 0) || ignored.test(i)) {
                continue;
            }
            if (open >= 0) {
                if (depth == MAX_DEPTH) {
                    break;
                }
                kinds[depth] = open;
                lines[depth] = line;
                depth++;
            } else if (depth > 0 && kinds[depth - 1] == close) {
                depth--;
                int start = lines[depth];
                if (line - start >= 2) {
                    byStart.merge(start, line, Math::max);
                }
            }
        }
        List<Range> result = new ArrayList<>(byStart.size());
        for (Map.Entry<Integer, Integer> entry : byStart.entrySet()) {
            result.add(new Range(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    public static Range innermost(List<Range> ranges, int line, Predicate<Range> accept) {
        for (int i = ranges.size() - 1; i >= 0; i--) {
            Range range = ranges.get(i);
            if (range.startLine() <= line && range.endLine() >= line && accept.test(range)) {
                return range;
            }
        }
        return null;
    }

    public static Scan scan(List<Range> ranges, IntPredicate hidden, int lineCount) {
        Map<Integer, Range> byStart = new HashMap<>();
        for (Range range : ranges) {
            byStart.put(range.startLine(), range);
        }
        List<Integer> stale = new ArrayList<>();
        int runs = 0;
        int line = 0;
        while (line < lineCount) {
            if (!hidden.test(line)) {
                line++;
                continue;
            }
            int first = line;
            while (line + 1 < lineCount && hidden.test(line + 1)) {
                line++;
            }
            runs++;
            if (first > 0) {
                Range range = byStart.get(first - 1);
                if (range == null || range.lastHiddenLine() != line) {
                    stale.add(first - 1);
                }
            }
            line++;
        }
        return new Scan(stale, runs);
    }
}