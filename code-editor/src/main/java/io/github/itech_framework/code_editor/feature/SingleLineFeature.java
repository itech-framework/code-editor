package io.github.itech_framework.code_editor.feature;

import io.github.itech_framework.code_editor.CodeEditor;
import io.github.itech_framework.code_editor.EditorFeature;
import io.github.itech_framework.code_editor.internal.SingleLineText;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.IndexRange;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public final class SingleLineFeature implements EditorFeature {

	public static final String ID = "single-line";

	private final EventHandler<KeyEvent> pressedHandler = this::onKeyPressed;

	private final EventHandler<KeyEvent> typedHandler = this::onKeyTyped;

	private CodeEditor editor;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void install(CodeEditor target) {
		editor = target;
		target.addEventFilter(KeyEvent.KEY_PRESSED, pressedHandler);
		target.addEventFilter(KeyEvent.KEY_TYPED, typedHandler);
	}

	@Override
	public void uninstall(CodeEditor target) {
		target.removeEventFilter(KeyEvent.KEY_PRESSED, pressedHandler);
		target.removeEventFilter(KeyEvent.KEY_TYPED, typedHandler);
		editor = null;
	}

	private void onKeyPressed(KeyEvent event) {
		CodeEditor target = editor;
		if (target == null || !target.isSingleLine() || event.isConsumed() || !target.isTextAreaEvent(event)) {
			return;
		}
		if (event.getCode() == KeyCode.ENTER) {
			event.consume();
			Event.fireEvent(target, new ActionEvent(target, target));
			return;
		}
		if (isPaste(event) && target.isEditable() && Clipboard.getSystemClipboard().hasString()) {
			event.consume();
			paste(target, Clipboard.getSystemClipboard().getString());
		}
	}

	private void onKeyTyped(KeyEvent event) {
		CodeEditor target = editor;
		if (target == null || !target.isSingleLine() || event.isConsumed() || !target.isTextAreaEvent(event)) {
			return;
		}
		String typed = event.getCharacter();
		if (typed != null && (typed.indexOf('\n') >= 0 || typed.indexOf('\r') >= 0 || typed.indexOf('\t') >= 0)) {
			event.consume();
		}
	}

	private static boolean isPaste(KeyEvent event) {
		boolean shortcut = event.getCode() == KeyCode.V && event.isShortcutDown() && !event.isAltDown();
		boolean insert = event.getCode() == KeyCode.INSERT && event.isShiftDown() && !event.isShortcutDown();
		return shortcut || insert;
	}

	private static void paste(CodeEditor target, String text) {
		String clean = SingleLineText.strip(text);
		IndexRange selection = target.getSelection();
		target.replaceText(selection.getStart(), selection.getEnd(), clean);
		target.moveCaret(selection.getStart() + clean.length());
	}
}