package io.github.itech_framework.code_editor.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.fxmisc.richtext.CaretNode;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.SelectionImpl;

import javafx.scene.control.IndexRange;

public final class ExtraCarets {

    public static final int MAX = 200;

    private record Entry(CaretNode caret, SelectionImpl<Collection<String>, String, Collection<String>> selection) {
    }

    private final CodeArea area;

    private final List<Entry> entries = new ArrayList<>();

    private int counter;

    public ExtraCarets(CodeArea area) {
        this.area = area;
    }

    public boolean add(int position) {
        int offset = clamp(position);
        if (entries.size() >= MAX || offset == area.getCaretPosition() || indexOf(offset) >= 0) {
            return false;
        }
        CaretNode caret = new CaretNode("extra-caret-" + (++counter), area, offset);
        area.addCaret(caret);
        entries.add(new Entry(caret, null));
        return true;
    }

    public boolean addRange(int anchor, int caretPosition) {
        int from = clamp(Math.min(anchor, caretPosition));
        int to = clamp(Math.max(anchor, caretPosition));
        if (from == to) {
            return add(from);
        }
        if (entries.size() >= MAX || overlapsAny(from, to)) {
            return false;
        }
        int id = ++counter;
        CaretNode caret = new CaretNode("extra-caret-" + id, area, clamp(caretPosition));
        SelectionImpl<Collection<String>, String, Collection<String>> selection = new SelectionImpl<>(
                "extra-selection-" + id, area, from, to);
        area.addCaret(caret);
        area.addSelection(selection);
        entries.add(new Entry(caret, selection));
        return true;
    }

    public void toggle(int position) {
        int offset = clamp(position);
        int index = indexOf(offset);
        if (index < 0) {
            add(offset);
            return;
        }
        remove(entries.remove(index));
    }

    public void clear() {
        for (Entry entry : entries) {
            remove(entry);
        }
        entries.clear();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public List<Integer> positions() {
        List<Integer> result = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            result.add(entry.caret().getPosition());
        }
        return result;
    }

    public List<IndexRange> ranges() {
        List<IndexRange> result = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            if (entry.selection() != null && entry.selection().getLength() > 0) {
                result.add(entry.selection().getRange());
            } else {
                int position = entry.caret().getPosition();
                result.add(new IndexRange(position, position));
            }
        }
        return result;
    }

    private void remove(Entry entry) {
        area.removeCaret(entry.caret());
        if (entry.selection() != null) {
            area.removeSelection(entry.selection());
        }
    }

    private boolean overlapsAny(int from, int to) {
        IndexRange primary = area.getSelection();
        if (overlaps(primary.getStart(), primary.getEnd(), from, to)) {
            return true;
        }
        for (IndexRange range : ranges()) {
            if (overlaps(range.getStart(), range.getEnd(), from, to)) {
                return true;
            }
        }
        return false;
    }

    private static boolean overlaps(int start, int end, int from, int to) {
        if (start == end) {
            return from < start && start < to;
        }
        return from < end && start < to;
    }

    private int clamp(int position) {
        return Math.max(0, Math.min(position, area.getLength()));
    }

    private int indexOf(int offset) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).caret().getPosition() == offset) {
                return i;
            }
        }
        return -1;
    }
}