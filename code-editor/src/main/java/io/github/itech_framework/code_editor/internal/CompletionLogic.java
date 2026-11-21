package io.github.itech_framework.code_editor.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.itech_framework.code_editor.completion.CompletionItem;

public final class CompletionLogic {

    public static final int MAX_ITEMS = 50;

    public static final int MIN_WORD_LENGTH = 3;

    public static final int MAX_SCAN = 200_000;

    private CompletionLogic() {
    }

    public static boolean isWordChar(char c) {
        return c == '$' || (Character.isUnicodeIdentifierPart(c) && !Character.isIdentifierIgnorable(c));
    }

    public static boolean isWordStart(char c) {
        return c == '$' || c == '_' || Character.isUnicodeIdentifierStart(c);
    }

    public static int wordStart(CharSequence text, int caret) {
        int start = Math.min(Math.max(caret, 0), text.length());
        while (start > 0 && isWordChar(text.charAt(start - 1))) {
            start--;
        }
        return start;
    }

    public static int wordEnd(CharSequence text, int caret) {
        int end = Math.min(Math.max(caret, 0), text.length());
        while (end < text.length() && isWordChar(text.charAt(end))) {
            end++;
        }
        return end;
    }
    
    public static int prefixLength(CharSequence before) {
        int start = before.length();
        while (start > 0 && isWordChar(before.charAt(start - 1))) {
            start--;
        }
        if (start == before.length()) {
            return 0;
        }
        return isWordStart(before.charAt(start)) ? before.length() - start : 0;
    }

    public static List<CompletionItem> suggest(String text, int caret, Collection<String> keywords) {
        if (caret < 0 || caret > text.length()) {
            return List.of();
        }
        int start = wordStart(text, caret);
        if (start == caret || !isWordStart(text.charAt(start))) {
            return List.of();
        }
        String prefix = text.substring(start, caret);
        Set<String> seen = new HashSet<>();
        List<CompletionItem> result = new ArrayList<>();
        List<String> matchedKeywords = new ArrayList<>();
        for (String keyword : keywords) {
            if (keyword.length() > prefix.length() && keyword.regionMatches(true, 0, prefix, 0, prefix.length())
                    && seen.add(keyword)) {
                matchedKeywords.add(keyword);
            }
        }
        matchedKeywords.sort(Comparator.comparing((String word) -> !word.startsWith(prefix))
                .thenComparing(String.CASE_INSENSITIVE_ORDER).thenComparing(Comparator.naturalOrder()));
        for (String keyword : matchedKeywords) {
            if (result.size() == MAX_ITEMS) {
                return result;
            }
            result.add(CompletionItem.of(keyword, CompletionItem.Kind.KEYWORD, start, caret));
        }
        for (String word : documentWords(text, caret, start, prefix, seen)) {
            if (result.size() == MAX_ITEMS) {
                break;
            }
            result.add(CompletionItem.of(word, CompletionItem.Kind.WORD, start, caret));
        }
        return result;
    }

    private static List<String> documentWords(String text, int caret, int currentStart, String prefix,
            Set<String> excluded) {
        int low = wordStart(text, Math.max(0, caret - MAX_SCAN / 2));
        int high = wordEnd(text, Math.min(text.length(), caret + MAX_SCAN / 2));
        Map<String, Integer> distances = new HashMap<>();
        int i = low;
        while (i < high) {
            if (!isWordChar(text.charAt(i))) {
                i++;
                continue;
            }
            int end = i + 1;
            while (end < text.length() && isWordChar(text.charAt(end))) {
                end++;
            }
            int length = end - i;
            if (i != currentStart && length >= MIN_WORD_LENGTH && length > prefix.length()
                    && isWordStart(text.charAt(i)) && text.regionMatches(true, i, prefix, 0, prefix.length())) {
                String word = text.substring(i, end);
                if (!excluded.contains(word)) {
                    int distance = end <= caret ? caret - end : i - caret;
                    distances.merge(word, distance, Math::min);
                }
            }
            i = end;
        }
        List<String> words = new ArrayList<>(distances.keySet());
        words.sort(Comparator.comparing((String word) -> !word.startsWith(prefix))
                .thenComparing(word -> distances.get(word))
                .thenComparing(String.CASE_INSENSITIVE_ORDER).thenComparing(Comparator.naturalOrder()));
        return words;
    }
}