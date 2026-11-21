package io.github.itech_framework.code_editor.internal;

import java.util.ArrayList;
import java.util.List;

public final class SelectionLogic {

    private SelectionLogic() {
    }

    public static boolean isWordChar(char c) {
        if (Character.isLetterOrDigit(c) || c == '_') {
            return true;
        }
        int type = Character.getType(c);
        return type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK;
    }

    public static MultiEditLogic.Span wordAt(CharSequence text, int offset) {
        int position = Math.max(0, Math.min(offset, text.length()));
        int start = position;
        int end = position;
        while (start > 0 && isWordChar(text.charAt(start - 1))) {
            start--;
        }
        while (end < text.length() && isWordChar(text.charAt(end))) {
            end++;
        }
        return end > start ? new MultiEditLogic.Span(start, end) : null;
    }

    public static int nextOccurrence(CharSequence text, String needle, int from, boolean wholeWord,
            List<MultiEditLogic.Span> taken) {
        String source = text.toString();
        int length = needle.length();
        if (length == 0 || length > source.length()) {
            return -1;
        }
        int start = Math.max(0, Math.min(from, source.length()));
        int index = source.indexOf(needle, start);
        while (index >= 0) {
            if (accepts(source, index, length, wholeWord, taken)) {
                return index;
            }
            index = source.indexOf(needle, index + 1);
        }
        index = source.indexOf(needle);
        while (index >= 0 && index < start) {
            if (accepts(source, index, length, wholeWord, taken)) {
                return index;
            }
            index = source.indexOf(needle, index + 1);
        }
        return -1;
    }

    public static List<MultiEditLogic.Span> allOccurrences(CharSequence text, String needle, boolean wholeWord,
            int limit) {
        List<MultiEditLogic.Span> result = new ArrayList<>();
        String source = text.toString();
        int length = needle.length();
        if (length == 0) {
            return result;
        }
        int index = source.indexOf(needle);
        while (index >= 0 && result.size() < limit) {
            if (!wholeWord || isBoundary(source, index, length)) {
                result.add(new MultiEditLogic.Span(index, index + length));
                index = source.indexOf(needle, index + length);
            } else {
                index = source.indexOf(needle, index + 1);
            }
        }
        return result;
    }

    public static List<Integer> lineEnds(CharSequence text, int start, int end) {
        List<Integer> result = new ArrayList<>();
        int stop = end > start && text.charAt(end - 1) == '\n' ? end - 1 : end;
        int position = start;
        while (true) {
            int lineEnd = position;
            while (lineEnd < text.length() && text.charAt(lineEnd) != '\n') {
                lineEnd++;
            }
            result.add(lineEnd);
            if (lineEnd >= stop || lineEnd >= text.length()) {
                break;
            }
            position = lineEnd + 1;
        }
        return result;
    }

    private static boolean accepts(String source, int index, int length, boolean wholeWord,
            List<MultiEditLogic.Span> taken) {
        if (wholeWord && !isBoundary(source, index, length)) {
            return false;
        }
        for (MultiEditLogic.Span span : taken) {
            if (overlaps(span, index, index + length)) {
                return false;
            }
        }
        return true;
    }

    private static boolean overlaps(MultiEditLogic.Span span, int start, int end) {
        if (span.start() == span.end()) {
            return start < span.start() && span.start() < end;
        }
        return start < span.end() && span.start() < end;
    }

    private static boolean isBoundary(String source, int index, int length) {
        boolean before = index == 0 || !isWordChar(source.charAt(index - 1));
        boolean after = index + length >= source.length() || !isWordChar(source.charAt(index + length));
        return before && after;
    }
}