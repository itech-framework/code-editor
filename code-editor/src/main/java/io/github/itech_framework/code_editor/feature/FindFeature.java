package io.github.itech_framework.code_editor.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.itech_framework.code_editor.CodeEditor;
import io.github.itech_framework.code_editor.EditorFeature;
import io.github.itech_framework.code_editor.internal.EditorInternals;
import io.github.itech_framework.code_editor.internal.FindLogic;
import io.github.itech_framework.code_editor.internal.FindPanel;
import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;

public final class FindFeature implements EditorFeature {

	public static final String ID = "find";

	public static final String MATCH_CLASS = "find-match";

	public static final String CURRENT_CLASS = "find-match-current";

	private static final int MAX_PREFILL = 200;

	private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
		Thread thread = new Thread(task, "code-editor-find");
		thread.setDaemon(true);
		return thread;
	});

	private final EventHandler<KeyEvent> keyHandler = this::onKeyPressed;

	private final ChangeListener<String> queryListener = (obs, old, value) -> schedule(true);

	private final ChangeListener<Boolean> optionListener = (obs, old, value) -> schedule(true);

	private final ChangeListener<Boolean> visibleListener = (obs, old, visible) -> onVisibleChanged(visible);

	private final ChangeListener<Integer> caretListener = (obs, old, value) -> updateStatus();

	private final PauseTransition idle = new PauseTransition(Duration.millis(250));

	private CodeEditor editor;

	private FindPanel panel;

	private final ChangeListener<Boolean> editableListener = (obs, old, editable) -> panel.setReplaceEnabled(editable);

	private Runnable stopTextListening;

	private Runnable stopHighlightListening;

	private List<int[]> matches = new ArrayList<>();

	private boolean truncated;

	private String error;

	private int currentIndex = -1;

	private int generation;

	private int pendingDirection;

	private boolean selectAfter;

	private boolean dirty;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void install(CodeEditor target) {
		editor = target;
		panel = EditorInternals.of(target).findPanel();
		idle.setOnFinished(event -> runSearch());
		target.addEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
		panel.queryProperty().addListener(queryListener);
		panel.caseSensitiveProperty().addListener(optionListener);
		panel.wholeWordProperty().addListener(optionListener);
		panel.regexProperty().addListener(optionListener);
		panel.setOnNext(() -> navigate(1));
		panel.setOnPrevious(() -> navigate(-1));
		panel.setOnReplace(this::replaceCurrent);
		panel.setOnReplaceAll(this::replaceAll);
		panel.setReplaceEnabled(target.isEditable());
		target.editableProperty().addListener(editableListener);
		target.findPanelVisibleProperty().addListener(visibleListener);
		target.caretPositionProperty().addListener(caretListener);
		stopTextListening = target.addTextChangeListener(this::onTextChanged);
		stopHighlightListening = target.addHighlightListener(this::reapply);
		if (target.isFindPanelVisible()) {
			onVisibleChanged(true);
		}
	}

	@Override
	public void uninstall(CodeEditor target) {
		idle.stop();
		generation++;
		target.removeEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
		panel.queryProperty().removeListener(queryListener);
		panel.caseSensitiveProperty().removeListener(optionListener);
		panel.wholeWordProperty().removeListener(optionListener);
		panel.regexProperty().removeListener(optionListener);
		panel.setOnNext(null);
		panel.setOnPrevious(null);
		panel.setOnReplace(null);
		panel.setOnReplaceAll(null);
		target.editableProperty().removeListener(editableListener);
		target.findPanelVisibleProperty().removeListener(visibleListener);
		target.caretPositionProperty().removeListener(caretListener);
		if (stopTextListening != null) {
			stopTextListening.run();
			stopTextListening = null;
		}
		if (stopHighlightListening != null) {
			stopHighlightListening.run();
			stopHighlightListening = null;
		}
		clearMarks();
		error = null;
		dirty = false;
		pendingDirection = 0;
		panel.setStatus("", false);
		target.setFindPanelVisible(false);
		editor = null;
		panel = null;
	}

	private void onKeyPressed(KeyEvent event) {
		CodeEditor target = editor;
		if (target == null) {
			return;
		}
		KeyCode code = event.getCode();
		if (code == KeyCode.F && event.isShortcutDown() && !event.isAltDown() && !event.isShiftDown()) {
			open();
			event.consume();
		} else if (code == KeyCode.H && event.isShortcutDown() && !event.isAltDown() && !event.isShiftDown()) {
			if (target.isEditable()) {
				panel.replaceVisibleProperty().set(true);
			}
			open();
			event.consume();
		} else if (code == KeyCode.F3 && !event.isControlDown() && !event.isAltDown() && !event.isMetaDown()) {
			if (!target.isFindPanelVisible()) {
				open();
			}
			navigate(event.isShiftDown() ? -1 : 1);
			event.consume();
		} else if (code == KeyCode.ESCAPE && target.isFindPanelVisible() && target.isTextAreaEvent(event)) {
			target.setFindPanelVisible(false);
			event.consume();
		}
	}

	private void open() {
		IndexRange selection = editor.getSelection();
		if (selection.getLength() > 0 && selection.getLength() <= MAX_PREFILL) {
			String text = editor.getText(selection.getStart(), selection.getEnd());
			if (text.indexOf('\n') < 0 && text.indexOf('\r') < 0) {
				panel.queryProperty().set(text);
			}
		}
		if (editor.isFindPanelVisible()) {
			panel.focusField();
		} else {
			editor.setFindPanelVisible(true);
		}
	}

	private void onVisibleChanged(boolean visible) {
		if (editor == null) {
			return;
		}
		if (visible) {
			if (!panel.queryProperty().get().isEmpty()) {
				schedule(true);
			}
			return;
		}
		idle.stop();
		generation++;
		pendingDirection = 0;
		selectAfter = false;
		dirty = false;
		error = null;
		clearMarks();
		panel.setStatus("", false);
	}

	private void schedule(boolean select) {
		if (editor == null || !editor.isFindPanelVisible()) {
			return;
		}
		selectAfter = selectAfter || select;
		dirty = true;
		generation++;
		idle.playFromStart();
	}

	private void runSearch() {
		CodeEditor target = editor;
		if (target == null) {
			return;
		}
		String query = panel.queryProperty().get();
		if (query == null || query.isEmpty()) {
			clearMarks();
			error = null;
			dirty = false;
			pendingDirection = 0;
			selectAfter = false;
			panel.setStatus("", false);
			return;
		}
		FindLogic.Options options = new FindLogic.Options(panel.caseSensitiveProperty().get(),
				panel.wholeWordProperty().get(), panel.regexProperty().get());
		String text = target.getText();
		boolean select = selectAfter;
		selectAfter = false;
		int ticket = generation;
		WORKER.execute(() -> {
			FindLogic.Result result;
			try {
				result = FindLogic.findAll(text, query, options);
			} catch (RuntimeException | StackOverflowError ex) {
				result = new FindLogic.Result(List.of(), "Search failed", false);
			}
			FindLogic.Result delivered = result;
			Platform.runLater(() -> accept(ticket, select, delivered));
		});
	}

	private void accept(int ticket, boolean select, FindLogic.Result result) {
		if (editor == null || ticket != generation || !editor.isFindPanelVisible()) {
			return;
		}
		dirty = false;
		clearMarks();
		error = result.error();
		if (error != null) {
			pendingDirection = 0;
			updateStatus();
			return;
		}
		matches = new ArrayList<>(result.matches());
		truncated = result.truncated();
		currentIndex = -1;
		for (int[] range : matches) {
			editor.addStyleClass(range[0], range[1], MATCH_CLASS);
		}
		if (pendingDirection != 0) {
			int direction = pendingDirection;
			pendingDirection = 0;
			step(direction);
		} else if (select && !matches.isEmpty()) {
			goTo(FindLogic.nextIndex(matches, editor.getSelection().getStart()));
		} else {
			IndexRange selection = editor.getSelection();
			int index = FindLogic.indexOf(matches, selection.getStart(), selection.getEnd());
			if (index >= 0) {
				markCurrent(index);
			}
		}
		updateStatus();
	}

	private void navigate(int direction) {
		if (editor == null || panel.queryProperty().get().isEmpty()) {
			return;
		}
		if (dirty) {
			pendingDirection = direction;
			if (idle.getStatus() == Animation.Status.RUNNING) {
				idle.stop();
				runSearch();
			}
			return;
		}
		step(direction);
	}

	private void step(int direction) {
		if (matches.isEmpty()) {
			return;
		}
		IndexRange selection = editor.getSelection();
		int index = direction > 0 ? FindLogic.nextIndex(matches, selection.getEnd())
				: FindLogic.previousIndex(matches, selection.getStart());
		goTo(index);
	}

	private void goTo(int index) {
		if (index < 0 || index >= matches.size()) {
			return;
		}
		int[] range = matches.get(index);
		if (range[0] < 0) {
			return;
		}
		editor.selectRange(range[0], range[1]);
		editor.revealSelection();
		markCurrent(index);
		updateStatus();
	}

	private void markCurrent(int index) {
		int length = editor.getLength();
		if (currentIndex >= 0 && currentIndex < matches.size()) {
			int[] old = matches.get(currentIndex);
			if (isValid(old, length)) {
				editor.removeStyleClass(old[0], old[1], CURRENT_CLASS);
			}
		}
		currentIndex = index;
		if (index >= 0 && index < matches.size()) {
			int[] range = matches.get(index);
			if (isValid(range, length)) {
				editor.addStyleClass(range[0], range[1], CURRENT_CLASS);
			}
		}
	}

	private void clearMarks() {
		CodeEditor target = editor;
		if (target != null) {
			int length = target.getLength();
			for (int[] range : matches) {
				if (isValid(range, length)) {
					target.removeStyleClass(range[0], range[1], MATCH_CLASS);
				}
			}
			if (currentIndex >= 0 && currentIndex < matches.size()) {
				int[] current = matches.get(currentIndex);
				if (isValid(current, length)) {
					target.removeStyleClass(current[0], current[1], CURRENT_CLASS);
				}
			}
		}
		matches = new ArrayList<>();
		truncated = false;
		currentIndex = -1;
	}

	private void reapply() {
		CodeEditor target = editor;
		if (target == null || matches.isEmpty()) {
			return;
		}
		int length = target.getLength();
		for (int[] range : matches) {
			if (isValid(range, length)) {
				target.addStyleClass(range[0], range[1], MATCH_CLASS);
			}
		}
		if (currentIndex >= 0 && currentIndex < matches.size()) {
			int[] current = matches.get(currentIndex);
			if (isValid(current, length)) {
				target.addStyleClass(current[0], current[1], CURRENT_CLASS);
			}
		}
	}

	private void onTextChanged(int position, int removed, int inserted) {
		int delta = inserted - removed;
		for (int[] range : matches) {
			if (range[0] < 0 || range[1] <= position) {
				continue;
			}
			if (range[0] >= position + removed) {
				range[0] += delta;
				range[1] += delta;
			} else {
				range[0] = -1;
				range[1] = -1;
			}
		}
		schedule(false);
	}

	private FindLogic.Options options() {
		return new FindLogic.Options(panel.caseSensitiveProperty().get(), panel.wholeWordProperty().get(),
				panel.regexProperty().get());
	}

	private void replaceCurrent() {
		CodeEditor target = editor;
		String query = panel.queryProperty().get();
		if (target == null || !target.isEditable() || query.isEmpty()) {
			return;
		}
		IndexRange selection = target.getSelection();
		if (selection.getLength() > 0) {
			FindLogic.Replacement result = FindLogic.replaceOne(target.getText(), query, options(),
					panel.replaceProperty().get(), selection.getStart(), selection.getEnd());
			if (result.error() != null) {
				error = result.error();
				updateStatus();
				return;
			}
			if (result.count() == 1) {
				target.replaceText(result.start(), result.end(), result.text());
				target.moveCaret(result.start() + result.text().length());
			}
		}
		navigate(1);
	}

	private void replaceAll() {
		CodeEditor target = editor;
		String query = panel.queryProperty().get();
		if (target == null || !target.isEditable() || query.isEmpty()) {
			return;
		}
		FindLogic.Replacement result = FindLogic.replaceAll(target.getText(), query, options(),
				panel.replaceProperty().get());
		if (result.error() != null) {
			error = result.error();
			updateStatus();
			return;
		}
		if (result.count() == 0) {
			return;
		}
		error = null;
		target.replaceText(result.start(), result.end(), result.text());
		target.moveCaret(result.start());
		panel.setStatus(result.count() + " replaced", false);
	}

	private void updateStatus() {
		if (editor == null || panel == null) {
			return;
		}
		if (error != null) {
			panel.setStatus(error, true);
			return;
		}
		if (panel.queryProperty().get().isEmpty()) {
			panel.setStatus("", false);
			return;
		}
		if (dirty) {
			return;
		}
		if (matches.isEmpty()) {
			panel.setStatus("No results", false);
			return;
		}
		IndexRange selection = editor.getSelection();
		int index = FindLogic.indexOf(matches, selection.getStart(), selection.getEnd());
		String position = index < 0 ? "?" : Integer.toString(index + 1);
		String total = truncated ? FindLogic.MAX_MATCHES + "+" : Integer.toString(matches.size());
		panel.setStatus(position + " of " + total, false);
	}

	private static boolean isValid(int[] range, int length) {
		return range[0] >= 0 && range[0] < range[1] && range[1] <= length;
	}
}