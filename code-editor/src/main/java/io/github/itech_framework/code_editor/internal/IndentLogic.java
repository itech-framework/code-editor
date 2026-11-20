package io.github.itech_framework.code_editor.internal;

public final class IndentLogic {

    private static final String OPENING = "([{";
    private static final String CLOSING = ")]}";

    public record Result(String insertion, int caretOffset, int consumedAfter) {
    }

    private IndentLogic() {
    }

    public static Result enter(String lineText, int column, String afterText, String indentUnit) {
        String leading = leadingWhitespace(lineText);
        String indent = leading.substring(0, Math.min(leading.length(), column));
        String before = lineText.substring(0, column);
        String trimmed = before.stripTrailing();
        int open = trimmed.isEmpty() ? -1 : OPENING.indexOf(trimmed.charAt(trimmed.length() - 1));
        String inner = open >= 0 ? indent + indentUnit : indent;
        String stripped = afterText.stripLeading();
        if (open >= 0 && !stripped.isEmpty() && stripped.charAt(0) == CLOSING.charAt(open)) {
            String insertion = "\n" + inner + "\n" + indent;
            return new Result(insertion, 1 + inner.length(), afterText.length() - stripped.length());
        }
        String insertion = "\n" + inner;
        return new Result(insertion, insertion.length(), 0);
    }

    private static String leadingWhitespace(String text) {
        int i = 0;
        while (i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == '\t')) {
            i++;
        }
        return text.substring(0, i);
    }
}