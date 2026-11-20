package io.github.itech_framework.code_editor.internal;

import java.util.ArrayList;
import java.util.List;

public final class LineOpsLogic {

    public record Edit(int start, int end, String text, int selectionStart, int selectionEnd) {
    }

    private record Block(int start, int end) {
    }

    private record Change(int position, int removed, int inserted) {
    }

    private LineOpsLogic() {
    }

    public static Edit deleteLines(String text, int selStart, int selEnd) {
        if (text.isEmpty()) {
            return null;
        }
        Block block = block(text, selStart, selEnd);
        int column = selStart - block.start();
        if (block.end() < text.length()) {
            int nextStart = block.end() + 1;
            int caret = block.start() + Math.min(column, lineEnd(text, nextStart) - nextStart);
            return new Edit(block.start(), nextStart, "", caret, caret);
        }
        if (block.start() == 0) {
            return new Edit(0, block.end(), "", 0, 0);
        }
        int previousStart = lineStart(text, block.start() - 1);
        int caret = previousStart + Math.min(column, block.start() - 1 - previousStart);
        return new Edit(block.start() - 1, block.end(), "", caret, caret);
    }

    public static Edit duplicateDown(String text, int selStart, int selEnd) {
        Block block = block(text, selStart, selEnd);
        String copy = text.substring(block.start(), block.end());
        int shift = copy.length() + 1;
        return new Edit(block.end(), block.end(), "\n" + copy, selStart + shift, selEnd + shift);
    }

    public static Edit duplicateUp(String text, int selStart, int selEnd) {
        Block block = block(text, selStart, selEnd);
        String copy = text.substring(block.start(), block.end());
        return new Edit(block.start(), block.start(), copy + "\n", selStart, selEnd);
    }

    public static Edit moveUp(String text, int selStart, int selEnd) {
        Block block = block(text, selStart, selEnd);
        if (block.start() == 0) {
            return null;
        }
        int previousStart = lineStart(text, block.start() - 1);
        String previous = text.substring(previousStart, block.start() - 1);
        String moved = text.substring(block.start(), block.end());
        int shift = -(previous.length() + 1);
        return new Edit(previousStart, block.end(), moved + "\n" + previous, selStart + shift, selEnd + shift);
    }

    public static Edit moveDown(String text, int selStart, int selEnd) {
        Block block = block(text, selStart, selEnd);
        if (block.end() >= text.length()) {
            return null;
        }
        int nextStart = block.end() + 1;
        int nextEnd = lineEnd(text, nextStart);
        String next = text.substring(nextStart, nextEnd);
        String moved = text.substring(block.start(), block.end());
        int shift = next.length() + 1;
        int limit = text.length();
        return new Edit(block.start(), nextEnd, next + "\n" + moved, Math.min(selStart + shift, limit),
                Math.min(selEnd + shift, limit));
    }

    public static Edit toggleComment(String text, int selStart, int selEnd, String prefix) {
        Block block = block(text, selStart, selEnd);
        String[] lines = text.substring(block.start(), block.end()).split("\n", -1);
        int[] indent = new int[lines.length];
        boolean[] blank = new boolean[lines.length];
        int minIndent = Integer.MAX_VALUE;
        int nonBlank = 0;
        boolean allCommented = true;
        for (int i = 0; i < lines.length; i++) {
            indent[i] = leadingWhitespace(lines[i]);
            blank[i] = indent[i] == lines[i].length();
            if (!blank[i]) {
                nonBlank++;
                minIndent = Math.min(minIndent, indent[i]);
                allCommented &= lines[i].startsWith(prefix, indent[i]);
            }
        }
        boolean uncomment = nonBlank > 0 && allCommented;
        int column = nonBlank == 0 ? 0 : minIndent;

        List<Change> changes = new ArrayList<>();
        StringBuilder result = new StringBuilder();
        int lineStart = block.start();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (i > 0) {
                result.append('\n');
            }
            if (nonBlank > 0 && blank[i]) {
                result.append(line);
            } else if (uncomment) {
                int removeEnd = indent[i] + prefix.length();
                if (removeEnd < line.length() && line.charAt(removeEnd) == ' ') {
                    removeEnd++;
                }
                result.append(line, 0, indent[i]).append(line, removeEnd, line.length());
                changes.add(new Change(lineStart + indent[i], removeEnd - indent[i], 0));
            } else {
                String insert = prefix + " ";
                result.append(line, 0, column).append(insert).append(line, column, line.length());
                changes.add(new Change(lineStart + column, 0, insert.length()));
            }
            lineStart += line.length() + 1;
        }
        return new Edit(block.start(), block.end(), result.toString(), map(changes, selStart, selStart < selEnd),
                map(changes, selEnd, false));
    }

    private static int map(List<Change> changes, int position, boolean stayBefore) {
        int shift = 0;
        for (Change change : changes) {
            if (stayBefore && change.removed() == 0 && position == change.position()) {
                break;
            }
            if (position >= change.position() + change.removed()) {
                shift += change.inserted() - change.removed();
            } else if (position > change.position()) {
                return change.position() + shift + change.inserted();
            } else {
                break;
            }
        }
        return position + shift;
    }

    private static int leadingWhitespace(String line) {
        int count = 0;
        while (count < line.length() && (line.charAt(count) == ' ' || line.charAt(count) == '\t')) {
            count++;
        }
        return count;
    }

    private static Block block(String text, int selStart, int selEnd) {
        int reference = selEnd;
        if (selEnd > selStart && selEnd > 0 && text.charAt(selEnd - 1) == '\n') {
            reference = selEnd - 1;
        }
        return new Block(lineStart(text, selStart), lineEnd(text, reference));
    }

    private static int lineStart(String text, int offset) {
        return offset <= 0 ? 0 : text.lastIndexOf('\n', offset - 1) + 1;
    }

    private static int lineEnd(String text, int offset) {
        int index = text.indexOf('\n', offset);
        return index < 0 ? text.length() : index;
    }
}