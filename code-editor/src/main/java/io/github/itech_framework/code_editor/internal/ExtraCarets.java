package io.github.itech_framework.code_editor.internal;

import java.util.ArrayList;
import java.util.List;

import org.fxmisc.richtext.CaretNode;
import org.fxmisc.richtext.CodeArea;

public final class ExtraCarets {

    public static final int MAX = 200;

    private final CodeArea area;

    private final List<CaretNode> carets = new ArrayList<>();

    private int counter;

    public ExtraCarets(CodeArea area) {
        this.area = area;
    }

    public boolean add(int position) {
        int offset = clamp(position);
        if (carets.size() >= MAX || offset == area.getCaretPosition() || indexOf(offset) >= 0) {
            return false;
        }
        CaretNode caret = new CaretNode("extra-caret-" + (++counter), area, offset);
        area.addCaret(caret);
        carets.add(caret);
        return true;
    }

    public void toggle(int position) {
        int offset = clamp(position);
        int index = indexOf(offset);
        if (index < 0) {
            add(offset);
            return;
        }
        area.removeCaret(carets.remove(index));
    }

    public void clear() {
        for (CaretNode caret : carets) {
            area.removeCaret(caret);
        }
        carets.clear();
    }

    public boolean isEmpty() {
        return carets.isEmpty();
    }

    public List<Integer> positions() {
        List<Integer> result = new ArrayList<>(carets.size());
        for (CaretNode caret : carets) {
            result.add(caret.getPosition());
        }
        return result;
    }

    private int clamp(int position) {
        return Math.max(0, Math.min(position, area.getLength()));
    }

    private int indexOf(int offset) {
        for (int i = 0; i < carets.size(); i++) {
            if (carets.get(i).getPosition() == offset) {
                return i;
            }
        }
        return -1;
    }
}