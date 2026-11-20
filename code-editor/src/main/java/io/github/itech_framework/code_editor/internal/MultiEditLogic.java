package io.github.itech_framework.code_editor.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.github.itech_framework.code_editor.TextEdit;

public final class MultiEditLogic {

    public record Span(int start, int end) {

        public Span {
            if (start < 0 || end < start) {
                throw new IllegalArgumentException("Invalid span: " + start + ".." + end);
            }
        }
    }

    public record Plan(List<TextEdit> edits, List<Integer> carets, int primary) {
    }

    private record Item(int start, int end, String text, boolean primary) {
    }

    private MultiEditLogic() {
    }

    public static Plan insert(CharSequence doc, List<Span> spans, int primary, String text) {
        check(doc, spans, primary);
        List<Item> items = new ArrayList<>(spans.size());
        for (int i = 0; i < spans.size(); i++) {
            Span span = spans.get(i);
            items.add(new Item(span.start(), span.end(), text, i == primary));
        }
        return finish(items);
    }

    public static Plan backspace(CharSequence doc, List<Span> spans, int primary) {
        check(doc, spans, primary);
        List<Item> items = new ArrayList<>(spans.size());
        for (int i = 0; i < spans.size(); i++) {
            Span span = spans.get(i);
            int start = span.start();
            if (span.end() > start) {
                items.add(new Item(start, span.end(), "", i == primary));
            } else if (start == 0) {
                items.add(new Item(0, 0, "", i == primary));
            } else {
                int width = start >= 2 && Character.isLowSurrogate(doc.charAt(start - 1))
                        && Character.isHighSurrogate(doc.charAt(start - 2)) ? 2 : 1;
                items.add(new Item(start - width, start, "", i == primary));
            }
        }
        return finish(items);
    }

    public static Plan delete(CharSequence doc, List<Span> spans, int primary) {
        check(doc, spans, primary);
        List<Item> items = new ArrayList<>(spans.size());
        for (int i = 0; i < spans.size(); i++) {
            Span span = spans.get(i);
            int start = span.start();
            if (span.end() > start) {
                items.add(new Item(start, span.end(), "", i == primary));
            } else if (start >= doc.length()) {
                items.add(new Item(start, start, "", i == primary));
            } else {
                int width = Character.isHighSurrogate(doc.charAt(start)) && start + 1 < doc.length()
                        && Character.isLowSurrogate(doc.charAt(start + 1)) ? 2 : 1;
                items.add(new Item(start, start + width, "", i == primary));
            }
        }
        return finish(items);
    }

    private static void check(CharSequence doc, List<Span> spans, int primary) {
        if (spans.isEmpty() || primary < 0 || primary >= spans.size()) {
            throw new IllegalArgumentException("Invalid primary index " + primary + " for " + spans.size() + " spans");
        }
        for (Span span : spans) {
            if (span.end() > doc.length()) {
                throw new IllegalArgumentException("Span " + span + " is outside the document");
            }
        }
    }

    private static Plan finish(List<Item> items) {
        List<Item> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparingInt(Item::start).thenComparingInt(Item::end));
        List<Item> merged = new ArrayList<>();
        for (Item item : sorted) {
            int last = merged.size() - 1;
            if (last >= 0) {
                Item previous = merged.get(last);
                if (item.start() < previous.end() || item.start() == previous.start()) {
                    merged.set(last, new Item(previous.start(), Math.max(previous.end(), item.end()),
                            previous.text(), previous.primary() || item.primary()));
                    continue;
                }
            }
            merged.add(item);
        }
        List<TextEdit> edits = new ArrayList<>();
        List<Integer> carets = new ArrayList<>();
        int primaryIndex = 0;
        int delta = 0;
        for (Item item : merged) {
            int newStart = item.start() + delta;
            int caret = newStart + item.text().length();
            if (item.start() != item.end() || !item.text().isEmpty()) {
                edits.add(new TextEdit(item.start(), item.end(), item.text()));
            }
            delta += item.text().length() - (item.end() - item.start());
            if (carets.isEmpty() || carets.get(carets.size() - 1) != caret) {
                carets.add(caret);
            }
            if (item.primary()) {
                primaryIndex = carets.size() - 1;
            }
        }
        return new Plan(List.copyOf(edits), List.copyOf(carets), primaryIndex);
    }
}