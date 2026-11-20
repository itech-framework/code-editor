package io.github.itech_framework.code_editor.internal;

import java.util.List;

import io.github.itech_framework.code_editor.language.BracketPair;

public final class AutoCloseLogic {

    public enum Kind {
        NONE, PAIR, WRAP, OVERTYPE
    }

    public record Result(Kind kind, char close) {
    }

    private static final Result NONE = new Result(Kind.NONE, '\0');

    private static final String CLOSE_BEFORE = ";:.,=}])> ";

    private AutoCloseLogic() {
    }

    public static Result typed(char typed, String line, int column, boolean hasSelection, List<BracketPair> pairs) {
        BracketPair opening = null;
        BracketPair closing = null;
        for (BracketPair pair : pairs) {
            if (opening == null && pair.open() == typed) {
                opening = pair;
            }
            if (closing == null && pair.close() == typed) {
                closing = pair;
            }
        }
        if (hasSelection) {
            return opening != null ? new Result(Kind.WRAP, opening.close()) : NONE;
        }
        char next = column < line.length() ? line.charAt(column) : '\0';
        char prev = column > 0 ? line.charAt(column - 1) : '\0';
        if (closing != null && next == typed) {
            return new Result(Kind.OVERTYPE, typed);
        }
        if (opening == null) {
            return NONE;
        }
        if (opening.isSymmetric()) {
            if (isWordChar(prev) || prev == '\\') {
                return NONE;
            }
            if (unescapedCount(line, column, typed) % 2 == 1) {
                return NONE;
            }
        }
        return canCloseBefore(next) ? new Result(Kind.PAIR, opening.close()) : NONE;
    }

    public static boolean deletesPair(String line, int column, List<BracketPair> pairs) {
        if (column <= 0 || column >= line.length()) {
            return false;
        }
        char prev = line.charAt(column - 1);
        char next = line.charAt(column);
        for (BracketPair pair : pairs) {
            if (pair.open() == prev && pair.close() == next) {
                if (pair.isSymmetric() && unescapedCount(line, column - 1, prev) % 2 == 1) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    private static boolean canCloseBefore(char next) {
        return next == '\0' || Character.isWhitespace(next) || CLOSE_BEFORE.indexOf(next) >= 0;
    }

    private static boolean isWordChar(char c) {
        if (Character.isLetterOrDigit(c) || c == '_') {
            return true;
        }
        int type = Character.getType(c);
        return type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK;
    }

    private static int unescapedCount(String line, int end, char target) {
        int count = 0;
        for (int i = 0; i < end; i++) {
            char c = line.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == target) {
                count++;
            }
        }
        return count;
    }
}