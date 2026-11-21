package io.github.itech_framework.code_editor.internal;

import io.github.itech_framework.code_editor.TextEdit;

public final class FormatLogic {

    private FormatLogic() {
    }

    public static TextEdit diff(String oldText, String newText) {
        if (oldText.equals(newText)) {
            return null;
        }
        int max = Math.min(oldText.length(), newText.length());
        int prefix = 0;
        while (prefix < max && oldText.charAt(prefix) == newText.charAt(prefix)) {
            prefix++;
        }
        if (prefix > 0 && Character.isHighSurrogate(oldText.charAt(prefix - 1))) {
            prefix--;
        }
        int suffix = 0;
        while (suffix < max - prefix
                && oldText.charAt(oldText.length() - 1 - suffix) == newText.charAt(newText.length() - 1 - suffix)) {
            suffix++;
        }
        if (suffix > 0 && Character.isLowSurrogate(oldText.charAt(oldText.length() - suffix))) {
            suffix--;
        }
        return new TextEdit(prefix, oldText.length() - suffix, newText.substring(prefix, newText.length() - suffix));
    }

    public static int mapOffset(String oldText, String newText, int offset) {
        int clamped = Math.max(0, Math.min(offset, oldText.length()));
        int max = Math.min(oldText.length(), newText.length());
        int prefix = 0;
        while (prefix < max && oldText.charAt(prefix) == newText.charAt(prefix)) {
            prefix++;
        }
        if (clamped <= prefix) {
            return clamped;
        }
        int suffix = 0;
        while (suffix < max - prefix
                && oldText.charAt(oldText.length() - 1 - suffix) == newText.charAt(newText.length() - 1 - suffix)) {
            suffix++;
        }
        if (clamped >= oldText.length() - suffix) {
            return clamped + newText.length() - oldText.length();
        }
        int before = 0;
        for (int i = 0; i < clamped; i++) {
            if (!isSpace(oldText.charAt(i))) {
                before++;
            }
        }
        boolean beforeNext = clamped < oldText.length()
                && (!isSpace(oldText.charAt(clamped)) || inLeadingIndent(oldText, clamped));
        return beforeNext ? positionBefore(newText, before + 1) : positionAfter(newText, before);
    }

    private static boolean inLeadingIndent(String text, int offset) {
        int i = offset;
        while (i > 0 && (text.charAt(i - 1) == ' ' || text.charAt(i - 1) == '\t')) {
            i--;
        }
        return i == 0 || text.charAt(i - 1) == '\n';
    }

    private static int positionBefore(String text, int number) {
        int seen = 0;
        for (int i = 0; i < text.length(); i++) {
            if (!isSpace(text.charAt(i)) && ++seen == number) {
                return i;
            }
        }
        return text.length();
    }

    private static int positionAfter(String text, int number) {
        if (number == 0) {
            return 0;
        }
        int seen = 0;
        for (int i = 0; i < text.length(); i++) {
            if (!isSpace(text.charAt(i)) && ++seen == number) {
                return i + 1;
            }
        }
        return text.length();
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }
}