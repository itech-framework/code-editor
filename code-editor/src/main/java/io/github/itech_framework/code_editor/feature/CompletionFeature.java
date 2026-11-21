package io.github.itech_framework.code_editor.feature;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.itech_framework.code_editor.CodeEditor;
import io.github.itech_framework.code_editor.EditorFeature;
import io.github.itech_framework.code_editor.completion.CompletionItem;
import io.github.itech_framework.code_editor.completion.CompletionProvider;
import io.github.itech_framework.code_editor.internal.CompletionLogic;
import io.github.itech_framework.code_editor.internal.CompletionPopup;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;

public final class CompletionFeature implements EditorFeature {

    public static final String ID = "completion";

    private static final int MAX_ITEMS = 200;

    private static final int PREFIX_WINDOW = 128;

    private static final System.Logger LOG = System.getLogger(CompletionFeature.class.getName());

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "code-editor-completion");
        thread.setDaemon(true);
        return thread;
    });

    private final EventHandler<KeyEvent> keyHandler = this::onKeyPressed;

    private final EventHandler<KeyEvent> typedHandler = this::onKeyTyped;

    private final EventHandler<MouseEvent> mouseHandler = this::onMousePressed;

    private final EventHandler<ScrollEvent> scrollHandler = this::onScroll;

    private final ChangeListener<Integer> caretListener = (obs, old, value) -> scheduleRefresh();

    private final ChangeListener<Boolean> focusListener = (obs, old, focused) -> {
        if (!focused) {
            close();
        }
    };

    private final ChangeListener<Object> resetListener = (obs, old, value) -> close();

    private CodeEditor editor;

    private CompletionPopup popup;

    private Runnable stopTextListening;

    private int generation;

    private int suppressedStart = -1;

    private boolean open;

    private boolean stale;

    private boolean pending;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void install(CodeEditor target) {
        editor = target;
        popup = target.completionPopup();
        popup.setOnAccept(item -> {
            if (acceptItem(target, item)) {
                target.requestFocus();
            }
        });
        target.addEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
        target.addEventFilter(KeyEvent.KEY_TYPED, typedHandler);
        target.addEventFilter(MouseEvent.MOUSE_PRESSED, mouseHandler);
        target.addEventFilter(ScrollEvent.SCROLL, scrollHandler);
        target.caretPositionProperty().addListener(caretListener);
        target.textFocusedProperty().addListener(focusListener);
        target.languageProperty().addListener(resetListener);
        target.completionProviderProperty().addListener(resetListener);
        stopTextListening = target.addTextChangeListener((position, removed, inserted) -> {
            generation++;
            stale = true;
            scheduleRefresh();
        });
    }

    @Override
    public void uninstall(CodeEditor target) {
        close();
        popup.setOnAccept(null);
        target.removeEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
        target.removeEventFilter(KeyEvent.KEY_TYPED, typedHandler);
        target.removeEventFilter(MouseEvent.MOUSE_PRESSED, mouseHandler);
        target.removeEventFilter(ScrollEvent.SCROLL, scrollHandler);
        target.caretPositionProperty().removeListener(caretListener);
        target.textFocusedProperty().removeListener(focusListener);
        target.languageProperty().removeListener(resetListener);
        target.completionProviderProperty().removeListener(resetListener);
        if (stopTextListening != null) {
            stopTextListening.run();
            stopTextListening = null;
        }
        popup = null;
        editor = null;
    }

    private void onKeyPressed(KeyEvent event) {
        CodeEditor target = editor;
        if (target == null || event.isConsumed() || !target.isTextAreaEvent(event)) {
            return;
        }
        KeyCode code = event.getCode();
        boolean plain = !event.isControlDown() && !event.isAltDown() && !event.isMetaDown()
                && !event.isShiftDown();
        if (open && plain) {
            if (code == KeyCode.UP) {
                popup.move(-1);
                event.consume();
                return;
            }
            if (code == KeyCode.DOWN) {
                popup.move(1);
                event.consume();
                return;
            }
            if (code == KeyCode.ESCAPE) {
                suppressedStart = currentWordStart(target);
                close();
                event.consume();
                return;
            }
            if ((code == KeyCode.ENTER || code == KeyCode.TAB) && accept(target)) {
                event.consume();
                return;
            }
        }
        if (code == KeyCode.SPACE && event.isControlDown() && !event.isAltDown() && !event.isMetaDown()
                && !event.isShiftDown()) {
            event.consume();
            open = true;
            request();
        }
    }

    private void onKeyTyped(KeyEvent event) {
        CodeEditor target = editor;
        if (target == null || open || !target.isCompletionAutoOpen() || !target.isEditable()
                || !target.isTextAreaEvent(event) || event.isControlDown() || event.isAltDown()
                || event.isMetaDown()) {
            return;
        }
        String typed = event.getCharacter();
        if (typed == null || typed.isEmpty()) {
            return;
        }
        CompletionProvider provider = target.effectiveCompletionProvider();
        if (provider == null) {
            return;
        }
        char c = typed.charAt(0);
        String triggers = provider.triggerCharacters();
        boolean trigger = triggers != null && triggers.indexOf(c) >= 0;
        if (!trigger && !CompletionLogic.isWordChar(c)) {
            return;
        }
        Platform.runLater(() -> autoOpen(target, trigger));
    }

    private void autoOpen(CodeEditor target, boolean trigger) {
        if (editor != target || open || !target.isEditable() || target.hasExtraCarets()
                || target.getSelection().getLength() != 0) {
            return;
        }
        if (!trigger) {
            int caret = target.getCaretPosition();
            int length = prefixLength(target);
            if (length < target.getCompletionMinChars()) {
                return;
            }
            if (caret - length == suppressedStart) {
                return;
            }
            suppressedStart = -1;
            if (inStringOrComment(target, caret - 1)) {
                return;
            }
        }
        open = true;
        request();
    }

    private void onMousePressed(MouseEvent event) {
        CodeEditor target = editor;
        if (open && target != null && target.isTextAreaEvent(event)) {
            close();
        }
    }

    private void onScroll(ScrollEvent event) {
        CodeEditor target = editor;
        if (open && target != null && target.isTextAreaEvent(event)) {
            close();
        }
    }

    private void scheduleRefresh() {
        if (!open || pending) {
            return;
        }
        pending = true;
        Platform.runLater(() -> {
            pending = false;
            if (open) {
                request();
            }
        });
    }

    private void request() {
        CodeEditor target = editor;
        if (target == null) {
            return;
        }
        CompletionProvider provider = target.effectiveCompletionProvider();
        if (provider == null || !target.isEditable() || target.hasExtraCarets()
                || target.getSelection().getLength() != 0) {
            close();
            return;
        }
        String text = target.getText();
        int caret = target.getCaretPosition();
        int token = ++generation;
        WORKER.execute(() -> {
            List<CompletionItem> items;
            try {
                List<CompletionItem> raw = provider.complete(text, caret);
                items = raw == null ? List.of() : List.copyOf(raw);
            } catch (RuntimeException ex) {
                LOG.log(System.Logger.Level.WARNING, "Completion provider failed", ex);
                items = List.of();
            }
            List<CompletionItem> result = items.size() > MAX_ITEMS ? items.subList(0, MAX_ITEMS) : items;
            Platform.runLater(() -> {
                if (editor == target && generation == token) {
                    show(target, result);
                }
            });
        });
    }

    private void show(CodeEditor target, List<CompletionItem> items) {
        stale = false;
        if (!open || items.isEmpty()) {
            close();
            return;
        }
        Optional<Bounds> caretBounds = target.getCaretBoundsOnScreen();
        if (caretBounds.isEmpty() || !(popup.getParent() instanceof Region parent)) {
            close();
            return;
        }
        Bounds caret = caretBounds.get();
        int start = items.get(0).start();
        double screenX = caret.getMinX();
        if (start < target.getCaretPosition()) {
            Optional<Bounds> startBounds = target.getCharacterBoundsOnScreen(start, start + 1);
            if (startBounds.isPresent()) {
                screenX = startBounds.get().getMinX();
            }
        }
        Point2D below = parent.screenToLocal(screenX, caret.getMaxY());
        Point2D above = parent.screenToLocal(screenX, caret.getMinY());
        if (below == null || above == null) {
            close();
            return;
        }
        CompletionItem previous = popup.isVisible() ? popup.selectedItem() : null;
        int selected = 0;
        if (previous != null) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).label().equals(previous.label())) {
                    selected = i;
                    break;
                }
            }
        }
        popup.show(items, selected, below.getX(), below.getY(), above.getY(), parent.getWidth(),
                parent.getHeight());
    }

    private boolean accept(CodeEditor target) {
        return acceptItem(target, popup.selectedItem());
    }

    private boolean acceptItem(CodeEditor target, CompletionItem item) {
        if (item == null || stale || item.end() != target.getCaretPosition() || item.end() > target.getLength()) {
            return false;
        }
        close();
        target.replaceText(item.start(), item.end(), item.insertText());
        target.moveCaret(item.start() + item.insertText().length());
        return true;
    }

    private void close() {
        open = false;
        generation++;
        if (popup != null) {
            popup.dismiss();
        }
    }

    private static int prefixLength(CodeEditor target) {
        int caret = target.getCaretPosition();
        return CompletionLogic.prefixLength(target.getText(Math.max(0, caret - PREFIX_WINDOW), caret));
    }

    private static int currentWordStart(CodeEditor target) {
        int length = prefixLength(target);
        return length == 0 ? -1 : target.getCaretPosition() - length;
    }

    private static boolean inStringOrComment(CodeEditor target, int offset) {
        if (offset < 0 || offset >= target.getLength()) {
            return false;
        }
        Collection<String> classes = target.getStyleClassesAt(offset);
        return classes.contains("tok-string") || classes.contains("tok-comment");
    }
}