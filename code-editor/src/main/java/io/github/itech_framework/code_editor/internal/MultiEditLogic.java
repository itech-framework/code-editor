package io.github.itech_framework.code_editor.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

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

    private record Item(int start, int end, String text, int caretOffset, boolean primary) {
    }

    private MultiEditLogic() {
    }

    public static Plan insert(CharSequence doc, List<Span> spans, int primary, String text) {
        check(doc, spans, primary);
        List<Item> items = new ArrayList<>(spans.size());
        for (int i = 0; i < spans.size(); i++) {
            Span span = spans.get(i);
            items.add(plain(span.start(), span.end(), text, i == primary));
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
                items.add(plain(start, span.end(), "", i == primary));
            } else if (start == 0) {
                items.add(plain(0, 0, "", i == primary));
            } else {
                int width = start >= 2 && Character.isLowSurrogate(doc.charAt(start - 1))
                        && Character.isHighSurrogate(doc.charAt(start - 2)) ? 2 : 1;
                items.add(plain(start - width, start, "", i == primary));
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
                items.add(plain(start, span.end(), "", i == primary));
            } else if (start >= doc.length()) {
                items.add(plain(start, start, "", i == primary));
            } else {
                int width = Character.isHighSurrogate(doc.charAt(start)) && start + 1 < doc.length()
                        && Character.isLowSurrogate(doc.charAt(start + 1)) ? 2 : 1;
                items.add(plain(start, start + width, "", i == primary));
            }
        }
        return finish(items);
    }

    public static Plan enter(CharSequence doc, List<Span> spans, int primary, String indentUnit) {
        check(doc, spans, primary);
        List<Item> items = new ArrayList<>(spans.size());
        for (int i = 0; i < spans.size(); i++) {
            Span span = spans.get(i);
            int lineStart = lineStart(doc, span.start());
            String lineText = doc.subSequence(lineStart, lineEnd(doc, span.start())).toString();
            int column = span.start() - lineStart;
            String after = doc.subSequence(span.end(), lineEnd(doc, span.end())).toString();
            IndentLogic.Result result = IndentLogic.enter(lineText, column, after, indentUnit);
            items.add(new Item(span.start(), span.end() + result.consumedAfter(), result.insertion(),
                    result.caretOffset(), i == primary));
        }
        return finish(items);
    }

    public static Plan tab(CharSequence doc, List<Span> spans, int primary, int tabSize, boolean spaces) {
        check(doc, spans, primary);
        List<Item> items = new ArrayList<>(spans.size());
        for (int i = 0; i < spans.size(); i++) {
            Span span = spans.get(i);
            String head = doc.subSequence(lineStart(doc, span.start()), span.start()).toString();
            String text = TabLogic.insertion(head, head.length(), tabSize, spaces);
            items.add(plain(span.start(), span.end(), text, i == primary));
        }
        return finish(items);
    }

    public static Plan outdent(CharSequence doc, List<Span> spans, int primary, int tabSize) {
        check(doc, spans, primary);
        TreeSet<Integer> lines = new TreeSet<>();
        for (Span span : spans) {
            int line = lineStart(doc, span.start());
            int last = lineStart(doc, span.end());
            if (last > line && span.end() == last) {
                last = lineStart(doc, last - 1);
            }
            while (true) {
                lines.add(line);
                if (line >= last) {
                    break;
                }
                line = lineEnd(doc, line) + 1;
            }
        }
        List<TextEdit> edits = new ArrayList<>();
        for (int line : lines) {
            int limit = Math.min(lineEnd(doc, line), line + tabSize);
            int removed = TabLogic.removable(doc.subSequence(line, limit).toString(), tabSize);
            if (removed > 0) {
                edits.add(new TextEdit(line, line + removed, ""));
            }
        }
        int primaryCaret = map(edits, spans.get(primary).end());
        TreeSet<Integer> carets = new TreeSet<>();
        for (Span span : spans) {
            carets.add(map(edits, span.end()));
        }
        List<Integer> list = new ArrayList<>(carets);
        return new Plan(List.copyOf(edits), List.copyOf(list), list.indexOf(primaryCaret));
    }

    private static Item plain(int start, int end, String text, boolean primary) {
        return new Item(start, end, text, text.length(), primary);
    }

    private static int map(List<TextEdit> edits, int offset) {
        int removed = 0;
        for (TextEdit edit : edits) {
            if (offset <= edit.start()) {
                break;
            }
            if (offset >= edit.end()) {
                removed += edit.end() - edit.start();
            } else {
                removed += offset - edit.start();
                break;
            }
        }
        return offset - removed;
    }

    private static int lineStart(CharSequence doc, int position) {
        int i = position;
        while (i > 0 && doc.charAt(i - 1) != '\n') {
            i--;
        }
        return i;
    }

    private static int lineEnd(CharSequence doc, int position) {
        int i = position;
        while (i < doc.length() && doc.charAt(i) != '\n') {
            i++;
        }
        return i;
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
                            previous.text(), previous.caretOffset(), previous.primary() || item.primary()));
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
            int caret = newStart + item.caretOffset();
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