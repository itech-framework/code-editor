package io.github.itech_framework.code_editor.internal;

public final class TabLogic {

    public record Block(String text, int start, int end) {
    }

    private TabLogic() {
    }

    public static String insertion(String lineText, int column, int tabSize, boolean spaces) {
        if (!spaces) {
            return "\t";
        }
        int visual = 0;
        for (int i = 0; i < column && i < lineText.length(); i++) {
            visual += lineText.charAt(i) == '\t' ? tabSize - (visual % tabSize) : 1;
        }
        return " ".repeat(tabSize - (visual % tabSize));
    }

    public static Block indent(String text, int start, int end, String unit) {
        String[] lines = text.split("\n", -1);
        StringBuilder out = new StringBuilder(text.length() + lines.length * unit.length());
        int newStart = start;
        int newEnd = end;
        int oldOffset = 0;
        int shift = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineEnd = oldOffset + line.length();
            int added = line.isEmpty() ? 0 : unit.length();
            if (start >= oldOffset && start <= lineEnd) {
                newStart = start + shift + (start > oldOffset ? added : 0);
            }
            if (end >= oldOffset && end <= lineEnd) {
                newEnd = end + shift + (end > oldOffset ? added : 0);
            }
            if (added > 0) {
                out.append(unit);
            }
            out.append(line);
            if (i < lines.length - 1) {
                out.append('\n');
            }
            shift += added;
            oldOffset = lineEnd + 1;
        }
        return new Block(out.toString(), newStart, newEnd);
    }

    public static Block outdent(String text, int start, int end, int tabSize) {
        String[] lines = text.split("\n", -1);
        StringBuilder out = new StringBuilder(text.length());
        int newStart = start;
        int newEnd = end;
        int oldOffset = 0;
        int shift = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineEnd = oldOffset + line.length();
            int removed = removable(line, tabSize);
            int newLineStart = oldOffset - shift;
            if (start >= oldOffset && start <= lineEnd) {
                newStart = newLineStart + Math.max(0, start - oldOffset - removed);
            }
            if (end >= oldOffset && end <= lineEnd) {
                newEnd = newLineStart + Math.max(0, end - oldOffset - removed);
            }
            out.append(line, removed, line.length());
            if (i < lines.length - 1) {
                out.append('\n');
            }
            shift += removed;
            oldOffset = lineEnd + 1;
        }
        return new Block(out.toString(), newStart, newEnd);
    }

    private static int removable(String line, int tabSize) {
        if (line.isEmpty()) {
            return 0;
        }
        if (line.charAt(0) == '\t') {
            return 1;
        }
        int count = 0;
        while (count < line.length() && count < tabSize && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }
}