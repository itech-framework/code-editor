package io.github.itech_framework.code_editor.internal;

public final class GoToLineLogic {

    private static final int MAX_DIGITS = 9;

    public record Target(int line, int column) {
    }

    public record Result(Target target, String error) {

        public boolean isEmpty() {
            return target == null && error == null;
        }
    }

    private GoToLineLogic() {
    }

    public static Result parse(String input, int lineCount) {
        String text = input == null ? "" : input.trim();
        if (text.isEmpty()) {
            return new Result(null, null);
        }
        int colon = text.indexOf(':');
        String linePart = (colon < 0 ? text : text.substring(0, colon)).trim();
        String columnPart = colon < 0 ? null : text.substring(colon + 1).trim();

        int line = number(linePart);
        if (line < 0) {
            return error("Enter a line number, or line:column");
        }
        if (line < 1 || line > lineCount) {
            return error("Line must be between 1 and " + lineCount);
        }
        int column = 1;
        if (columnPart != null) {
            column = number(columnPart);
            if (column < 0) {
                return error("Enter a line number, or line:column");
            }
            if (column < 1) {
                return error("Column must be 1 or more");
            }
        }
        return new Result(new Target(line, column), null);
    }

    public static int offset(int lineStart, int lineLength, int column) {
        return lineStart + Math.min(Math.max(column - 1, 0), lineLength);
    }

    private static Result error(String message) {
        return new Result(null, message);
    }

    private static int number(String text) {
        if (text.isEmpty() || text.length() > MAX_DIGITS) {
            return -1;
        }
        int value = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            value = value * 10 + (c - '0');
        }
        return value;
    }
}