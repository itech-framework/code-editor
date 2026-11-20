package io.github.itech_framework.code_editor.internal;

import java.util.function.IntPredicate;

public final class BracketLogic {

    public static final int MAX_SCAN = 20_000;

    private static final String OPENING = "([{";
    private static final String CLOSING = ")]}";

    private BracketLogic() {
    }

    public static int[] match(CharSequence text, int caret, IntPredicate ignored) {
        int[] found = matchAt(text, caret - 1, ignored);
        if (found == null) {
            found = matchAt(text, caret, ignored);
        }
        return found;
    }

    private static int[] matchAt(CharSequence text, int index, IntPredicate ignored) {
        if (index < 0 || index >= text.length()) {
            return null;
        }
        char c = text.charAt(index);
        int open = OPENING.indexOf(c);
        int close = CLOSING.indexOf(c);
        if ((open < 0 && close < 0) || ignored.test(index)) {
            return null;
        }
        if (open >= 0) {
            int other = scan(text, index, c, CLOSING.charAt(open), 1, ignored);
            return other < 0 ? null : new int[] { index, other };
        }
        int other = scan(text, index, c, OPENING.charAt(close), -1, ignored);
        return other < 0 ? null : new int[] { other, index };
    }

    private static int scan(CharSequence text, int from, char same, char opposite, int step, IntPredicate ignored) {
        int depth = 1;
        int index = from + step;
        for (int steps = 0; index >= 0 && index < text.length() && steps < MAX_SCAN; steps++, index += step) {
            char c = text.charAt(index);
            if ((c == same || c == opposite) && !ignored.test(index)) {
                if (c == same) {
                    depth++;
                } else if (--depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }
}