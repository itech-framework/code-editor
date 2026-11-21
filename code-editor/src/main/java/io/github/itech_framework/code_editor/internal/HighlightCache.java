package io.github.itech_framework.code_editor.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HighlightCache {

    public record Plan(int first, int end, int lastDirty, List<HighlightRun.LineInfo> window,
            HighlightRun.LineInfo previous, boolean more) {
    }

    private List<HighlightRun.LineInfo> infos = new ArrayList<>();

    private int dirtyFrom = Integer.MAX_VALUE;

    private int dirtyTo = -1;

    public int size() {
        return infos.size();
    }

    public HighlightRun.LineInfo get(int line) {
        return infos.get(line);
    }

    public void reset(int lineCount) {
        infos = new ArrayList<>(Collections.nCopies(lineCount, (HighlightRun.LineInfo) null));
        dirtyFrom = 0;
        dirtyTo = lineCount - 1;
    }

    public void edit(int line, int removed, int inserted) {
        infos.set(line, null);
        if (removed > 0) {
            int end = Math.min(infos.size(), line + 1 + removed);
            infos.subList(line + 1, end).clear();
        }
        if (inserted > 0) {
            infos.addAll(line + 1, Collections.nCopies(inserted, (HighlightRun.LineInfo) null));
        }
        int shifted = dirtyTo > line ? dirtyTo + inserted - removed : dirtyTo;
        dirtyTo = Math.min(Math.max(shifted, line + inserted), infos.size() - 1);
        dirtyFrom = Math.min(dirtyFrom, line);
    }

    public Plan plan(int windowLines) {
        if (dirtyFrom > dirtyTo) {
            return null;
        }
        int last = Math.min(dirtyTo, infos.size() - 1);
        int first = Math.max(0, dirtyFrom);
        while (first <= last && infos.get(first) != null) {
            first++;
        }
        if (first > last) {
            clearDirty();
            return null;
        }
        int lastDirty = last;
        while (infos.get(lastDirty) != null) {
            lastDirty--;
        }
        int end = (int) Math.min((long) infos.size(), (long) lastDirty + 1 + windowLines);
        List<HighlightRun.LineInfo> window = new ArrayList<>(infos.subList(first, end));
        HighlightRun.LineInfo previous = first > 0 ? infos.get(first - 1) : null;
        return new Plan(first, end, lastDirty - first, window, previous, end < infos.size());
    }

    public void apply(int first, List<HighlightRun.LineInfo> processed, boolean incomplete) {
        for (int k = 0; k < processed.size(); k++) {
            infos.set(first + k, processed.get(k));
        }
        int next = first + processed.size();
        if (incomplete && next < infos.size()) {
            infos.set(next, null);
            dirtyFrom = next;
            dirtyTo = next;
        } else {
            clearDirty();
        }
    }

    private void clearDirty() {
        dirtyFrom = Integer.MAX_VALUE;
        dirtyTo = -1;
    }
}