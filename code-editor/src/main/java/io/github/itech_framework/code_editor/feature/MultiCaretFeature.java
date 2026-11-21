package io.github.itech_framework.code_editor.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.github.itech_framework.code_editor.CodeEditor;
import io.github.itech_framework.code_editor.EditorFeature;
import io.github.itech_framework.code_editor.internal.ExtraCarets;
import io.github.itech_framework.code_editor.internal.MultiEditLogic;
import io.github.itech_framework.code_editor.internal.SelectionLogic;
import javafx.event.EventHandler;
import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public final class MultiCaretFeature implements EditorFeature {

    public static final String ID = "multi-caret";

    private static final Set<KeyCode> MODIFIERS = Set.of(KeyCode.SHIFT, KeyCode.CONTROL, KeyCode.ALT,
            KeyCode.ALT_GRAPH, KeyCode.META, KeyCode.WINDOWS, KeyCode.COMMAND, KeyCode.CAPS);

    private final EventHandler<MouseEvent> pressedHandler = this::onMousePressed;

    private final EventHandler<MouseEvent> followUpHandler = this::onMouseFollowUp;

    private final EventHandler<KeyEvent> keyHandler = this::onKeyPressed;

    private final EventHandler<KeyEvent> typedHandler = this::onKeyTyped;

    private CodeEditor editor;

    private boolean blocking;
    
    private String needle;

    private boolean wholeWord;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void install(CodeEditor target) {
        editor = target;
        target.addEventFilter(MouseEvent.MOUSE_PRESSED, pressedHandler);
        target.addEventFilter(MouseEvent.MOUSE_DRAGGED, followUpHandler);
        target.addEventFilter(MouseEvent.MOUSE_RELEASED, followUpHandler);
        target.addEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
        target.addEventFilter(KeyEvent.KEY_TYPED, typedHandler);
    }

    @Override
    public void uninstall(CodeEditor target) {
        target.removeEventFilter(MouseEvent.MOUSE_PRESSED, pressedHandler);
        target.removeEventFilter(MouseEvent.MOUSE_DRAGGED, followUpHandler);
        target.removeEventFilter(MouseEvent.MOUSE_RELEASED, followUpHandler);
        target.removeEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
        target.removeEventFilter(KeyEvent.KEY_TYPED, typedHandler);
        target.clearExtraCarets();
        blocking = false;
        editor = null;
    }

    private void onMousePressed(MouseEvent event) {
        CodeEditor target = editor;
        if (target == null || event.getButton() != MouseButton.PRIMARY || !target.isTextAreaEvent(event)) {
            return;
        }
        if (event.isAltDown() && !event.isShortcutDown() && !event.isShiftDown()) {
            target.requestFocus();
            target.toggleExtraCaret(target.offsetAt(event.getScreenX(), event.getScreenY()));
            blocking = true;
            event.consume();
            return;
        }
        target.clearExtraCarets();
    }

    private void onMouseFollowUp(MouseEvent event) {
        if (!blocking) {
            return;
        }
        event.consume();
        if (event.getEventType() == MouseEvent.MOUSE_RELEASED) {
            blocking = false;
        }
    }

    private void onKeyPressed(KeyEvent event) {
        CodeEditor target = editor;
        if (target == null || !target.isTextAreaEvent(event)) {
            return;
        }
        KeyCode code = event.getCode();
        if (MODIFIERS.contains(code)) {
            return;
        }
        boolean vertical = code == KeyCode.UP || code == KeyCode.DOWN;
        if (vertical && event.isShortcutDown() && event.isAltDown() && !event.isShiftDown()) {
            addVertically(target, code == KeyCode.UP ? -1 : 1);
            event.consume();
            return;
        }
        if (code == KeyCode.ESCAPE) {
            if (!event.isConsumed() && target.hasExtraCarets() && !target.isFindPanelVisible()
                    && !target.isGoToLineVisible()) {
                target.clearExtraCarets();
                event.consume();
            }
            return;
        }
        
        if (code == KeyCode.D && event.isShortcutDown() && !event.isAltDown() && !event.isShiftDown()) {
            selectNextOccurrence(target);
            event.consume();
            return;
        }
        if (code == KeyCode.L && event.isShortcutDown() && event.isShiftDown() && !event.isAltDown()) {
            selectAllOccurrences(target);
            event.consume();
            return;
        }
        if (code == KeyCode.I && event.isAltDown() && event.isShiftDown() && !event.isShortcutDown()) {
            addCaretsToLineEnds(target);
            event.consume();
            return;
        }
        
        if (target.hasExtraCarets() && target.isEditable() && isTypingChord(event)) {
            if (code == KeyCode.BACK_SPACE) {
                apply(target, MultiEditLogic.backspace(target.getText(), spans(target), 0));
                event.consume();
                return;
            }
            if (code == KeyCode.DELETE && !event.isShiftDown()) {
                apply(target, MultiEditLogic.delete(target.getText(), spans(target), 0));
                event.consume();
                return;
            }
            if (code == KeyCode.ENTER && !event.isShiftDown()) {
                apply(target, MultiEditLogic.enter(target.getText(), spans(target), 0, target.getIndentUnit()));
                event.consume();
                return;
            }
            if (code == KeyCode.TAB && !isMultiLine(target)) {
                String text = target.getText();
                MultiEditLogic.Plan plan = event.isShiftDown()
                        ? MultiEditLogic.outdent(text, spans(target), 0, target.getTabSize())
                        : MultiEditLogic.tab(text, spans(target), 0, target.getTabSize(), target.isInsertSpaces());
                apply(target, plan);
                event.consume();
                return;
            }
            if (isTypingKey(code)) {
                return;
            }
        }
        target.clearExtraCarets();
    }

    private void onKeyTyped(KeyEvent event) {
        CodeEditor target = editor;
        if (target == null || !target.hasExtraCarets() || !target.isEditable() || !target.isTextAreaEvent(event)
                || !isTypingChord(event)) {
            return;
        }
        String text = event.getCharacter();
        if (text.isEmpty() || Character.isISOControl(text.charAt(0))) {
            return;
        }
        apply(target, MultiEditLogic.insert(target.getText(), spans(target), 0, text));
        event.consume();
    }

    private static boolean isTypingChord(KeyEvent event) {
        if (event.isMetaDown()) {
            return false;
        }
        if (event.isControlDown()) {
            return event.isAltDown();
        }
        return !event.isAltDown();
    }

    private static boolean isTypingKey(KeyCode code) {
        return code != KeyCode.ENTER && code != KeyCode.TAB && !code.isNavigationKey() && !code.isArrowKey()
                && !code.isFunctionKey() && !code.isMediaKey();
    }
    
    private static boolean isMultiLine(CodeEditor target) {
        return target.hasMultiLineSelection();
    }

    private void selectNextOccurrence(CodeEditor target) {
        String text = target.getText();
        List<IndexRange> ranges = target.getSelectionRanges();
        IndexRange primary = ranges.get(0);
        if (primary.getLength() == 0) {
            MultiEditLogic.Span word = SelectionLogic.wordAt(text, primary.getStart());
            if (word == null) {
                return;
            }
            needle = text.substring(word.start(), word.end());
            wholeWord = true;
            target.setSelectionRanges(new IndexRange(word.start(), word.end()), ranges.subList(1, ranges.size()));
            target.revealSelection();
            return;
        }
        if (ranges.size() > ExtraCarets.MAX) {
            return;
        }
        String selected = text.substring(primary.getStart(), primary.getEnd());
        if (!selected.equals(needle)) {
            needle = selected;
            wholeWord = false;
        }
        List<MultiEditLogic.Span> taken = new ArrayList<>();
        int from = 0;
        for (IndexRange range : ranges) {
            taken.add(new MultiEditLogic.Span(range.getStart(), range.getEnd()));
            from = Math.max(from, range.getEnd());
        }
        int found = SelectionLogic.nextOccurrence(text, needle, from, wholeWord, taken);
        if (found < 0) {
            return;
        }
        target.setSelectionRanges(new IndexRange(found, found + needle.length()), ranges);
        target.revealSelection();
    }

    private void selectAllOccurrences(CodeEditor target) {
        String text = target.getText();
        IndexRange primary = target.getSelectionRanges().get(0);
        String term;
        boolean whole;
        if (primary.getLength() == 0) {
            MultiEditLogic.Span word = SelectionLogic.wordAt(text, primary.getStart());
            if (word == null) {
                return;
            }
            term = text.substring(word.start(), word.end());
            whole = true;
        } else {
            term = text.substring(primary.getStart(), primary.getEnd());
            whole = wholeWord && term.equals(needle);
        }
        List<MultiEditLogic.Span> all = SelectionLogic.allOccurrences(text, term, whole, ExtraCarets.MAX + 1);
        if (all.isEmpty()) {
            return;
        }
        int main = 0;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).start() <= primary.getStart() && primary.getStart() <= all.get(i).end()) {
                main = i;
                break;
            }
        }
        List<IndexRange> extras = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            if (i != main) {
                extras.add(new IndexRange(all.get(i).start(), all.get(i).end()));
            }
        }
        needle = term;
        wholeWord = whole;
        target.setSelectionRanges(new IndexRange(all.get(main).start(), all.get(main).end()), extras);
        target.revealSelection();
    }

    private void addCaretsToLineEnds(CodeEditor target) {
        IndexRange primary = target.getSelectionRanges().get(0);
        if (primary.getLength() == 0) {
            return;
        }
        List<Integer> ends = SelectionLogic.lineEnds(target.getText(), primary.getStart(), primary.getEnd());
        if (ends.size() < 2) {
            return;
        }
        if (ends.size() > ExtraCarets.MAX + 1) {
            ends = ends.subList(0, ExtraCarets.MAX + 1);
        }
        int last = ends.get(ends.size() - 1);
        List<IndexRange> extras = new ArrayList<>();
        for (int i = 0; i < ends.size() - 1; i++) {
            extras.add(new IndexRange(ends.get(i), ends.get(i)));
        }
        target.setSelectionRanges(new IndexRange(last, last), extras);
        target.revealSelection();
    }

    private static List<MultiEditLogic.Span> spans(CodeEditor target) {
        List<MultiEditLogic.Span> spans = new ArrayList<>();
        for (IndexRange range : target.getSelectionRanges()) {
            spans.add(new MultiEditLogic.Span(range.getStart(), range.getEnd()));
        }
        return spans;
    }

    private static void apply(CodeEditor target, MultiEditLogic.Plan plan) {
        target.applyEdits(plan.edits());
        List<Integer> carets = new ArrayList<>(plan.carets());
        int primary = carets.remove(plan.primary());
        target.setCaretPositions(primary, carets);
    }

    private void addVertically(CodeEditor target, int direction) {
        List<Integer> positions = target.getCaretPositions();
        int reference = direction < 0 ? Collections.min(positions) : Collections.max(positions);
        int line = target.getLineOfOffset(reference);
        int column = reference - target.getLineStart(line);
        int next = line + direction;
        while (next >= 0 && next < target.getLineCount() && target.isLineHidden(next)) {
            next += direction;
        }
        if (next < 0 || next >= target.getLineCount()) {
            return;
        }
        int offset = target.getLineStart(next) + Math.min(column, target.getLineText(next).length());
        target.addExtraCaret(offset);
    }
}