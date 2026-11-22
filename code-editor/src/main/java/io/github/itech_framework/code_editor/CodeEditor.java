package io.github.itech_framework.code_editor;

import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpan;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.TwoDimensional.Bias;
import org.fxmisc.richtext.util.UndoUtils;
import org.reactfx.Subscription;

import io.github.itech_framework.code_editor.completion.CompletionItem;
import io.github.itech_framework.code_editor.completion.CompletionProvider;
import io.github.itech_framework.code_editor.diagnostics.Diagnostic;
import io.github.itech_framework.code_editor.diagnostics.DiagnosticSeverity;
import io.github.itech_framework.code_editor.diagnostics.DiagnosticsProvider;
import io.github.itech_framework.code_editor.feature.AutoCloseFeature;
import io.github.itech_framework.code_editor.feature.AutoIndentFeature;
import io.github.itech_framework.code_editor.feature.BracketMatchFeature;
import io.github.itech_framework.code_editor.feature.CompletionFeature;
import io.github.itech_framework.code_editor.feature.DiagnosticHoverFeature;
import io.github.itech_framework.code_editor.feature.DiagnosticsFeature;
import io.github.itech_framework.code_editor.feature.FindFeature;
import io.github.itech_framework.code_editor.feature.FoldFeature;
import io.github.itech_framework.code_editor.feature.FormatFeature;
import io.github.itech_framework.code_editor.feature.GoToLineFeature;
import io.github.itech_framework.code_editor.feature.LineEditFeature;
import io.github.itech_framework.code_editor.feature.MultiCaretFeature;
import io.github.itech_framework.code_editor.feature.SingleLineFeature;
import io.github.itech_framework.code_editor.feature.TabFeature;
import io.github.itech_framework.code_editor.formatting.CodeFormatter;
import io.github.itech_framework.code_editor.internal.BuiltInCompletion;
import io.github.itech_framework.code_editor.internal.CompletionPopup;
import io.github.itech_framework.code_editor.internal.DiagnosticTooltip;
import io.github.itech_framework.code_editor.internal.EditorInternals;
import io.github.itech_framework.code_editor.internal.ExtraCarets;
import io.github.itech_framework.code_editor.internal.FindPanel;
import io.github.itech_framework.code_editor.internal.FoldMarkers;
import io.github.itech_framework.code_editor.internal.FoldState;
import io.github.itech_framework.code_editor.internal.FontSupport;
import io.github.itech_framework.code_editor.internal.GoToLineLogic;
import io.github.itech_framework.code_editor.internal.GoToLinePanel;
import io.github.itech_framework.code_editor.internal.LineNumberGutter;
import io.github.itech_framework.code_editor.internal.SingleLineText;
import io.github.itech_framework.code_editor.internal.SyntaxHighlighter;
import io.github.itech_framework.code_editor.language.EditorLanguage;
import io.github.itech_framework.code_editor.language.LanguageRegistry;
import io.github.itech_framework.code_editor.theme.EditorTheme;
import io.github.itech_framework.code_editor.theme.ThemeRegistry;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Skin;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class CodeEditor extends Control {

	public static final String DEFAULT_FONT_FAMILY = "JetBrains Mono, Cascadia Code, Consolas, Menlo, DejaVu Sans Mono, monospace";

	public static final double DEFAULT_FONT_SIZE = 14;

	public static final int DEFAULT_TAB_SIZE = 4;

	private static final System.Logger LOG = System.getLogger(CodeEditor.class.getName());

	private static final String BASE_STYLESHEET = "css/base.css";

	private static final String NO_CURRENT_LINE = "no-current-line";
	
	private static final String SINGLE_LINE_CLASS = "single-line";
	
	private static final double DEFAULT_PREF_HEIGHT = 400;

	private static final double SINGLE_LINE_PADDING = 4;

	private static final List<String> SINGLE_LINE_DISABLED = List.of("auto-indent", "auto-close", "tab-indent", "find",
			"go-to-line", "line-edit", "fold", "multi-caret", "format", "completion", "diagnostic-hover");

	private static volatile String baseStylesheetUrl;

	private final CodeArea area = new CodeArea();

	private final BooleanProperty showLineNumbers = new SimpleBooleanProperty(this, "showLineNumbers", true);

	private final BooleanProperty highlightCurrentLine = new SimpleBooleanProperty(this, "highlightCurrentLine", true);

	private final StringProperty fontFamily = new SimpleStringProperty(this, "fontFamily", DEFAULT_FONT_FAMILY);

	private final DoubleProperty fontSize = new SimpleDoubleProperty(this, "fontSize", DEFAULT_FONT_SIZE);

	private final StringProperty theme = new SimpleStringProperty(this, "theme", ThemeRegistry.DARK_PLUS);

	private final StringProperty language = new SimpleStringProperty(this, "language", LanguageRegistry.PLAIN_TEXT);

	private final IntegerProperty tabSize = new SimpleIntegerProperty(this, "tabSize", DEFAULT_TAB_SIZE);

	private final BooleanProperty insertSpaces = new SimpleBooleanProperty(this, "insertSpaces", true);

	private final BooleanProperty singleLine = new SimpleBooleanProperty(this, "singleLine", false);

	private final Set<String> disabledBySingleLine = new HashSet<>();
	
	private final EditorFeatures features = new EditorFeatures(this);

	private final ObjectProperty<DiagnosticsProvider> diagnosticsProvider = new SimpleObjectProperty<>(this,
			"diagnosticsProvider");

	private final ObjectProperty<CodeFormatter> formatter = new SimpleObjectProperty<>(this, "formatter");

	private final ObjectProperty<CompletionProvider> completionProvider = new SimpleObjectProperty<>(this,
			"completionProvider");
	
	private final BooleanProperty completionAutoOpen = new SimpleBooleanProperty(this, "completionAutoOpen", true);

	private final IntegerProperty completionMinChars = new SimpleIntegerProperty(this, "completionMinChars", 2);

	private final ReadOnlyObjectWrapper<List<Diagnostic>> diagnostics = new ReadOnlyObjectWrapper<>(this, "diagnostics",
			List.of());

	private final ReadOnlyIntegerWrapper errorCount = new ReadOnlyIntegerWrapper(this, "errorCount");

	private final FindPanel findPanel = new FindPanel();

	private final GoToLinePanel goToLinePanel = new GoToLinePanel();

	private final DiagnosticTooltip diagnosticTooltip = new DiagnosticTooltip();

	private final CompletionPopup completionPopup = new CompletionPopup();

	private final FoldState foldState = new FoldState();

	private final FoldMarkers foldMarkers = new FoldMarkers(area, FoldFeature.HEADER_CLASS, line -> toggleFold(line));

	private final ExtraCarets extraCarets = new ExtraCarets(area);

	private List<String> appliedStylesheets = List.of();

	private final Consumer<EditorTheme> themeListener = this::onThemeRegistered;

	private final SyntaxHighlighter highlighter;

	public CodeEditor() {
		getStyleClass().add("code-editor");
		setFocusTraversable(false);
		setMinSize(0, 0);
		setPrefSize(600, DEFAULT_PREF_HEIGHT);
		
		EditorInternals.attach(this, new EditorInternals(findPanel, goToLinePanel, diagnosticTooltip, completionPopup, foldState));
		
		area.setUndoManager(UndoUtils.plainTextUndoManager(area));
		highlighter = new SyntaxHighlighter(area, LanguageRegistry.getDefault().require(getLanguage()).getTokenizer());
		showLineNumbers.addListener((obs, old, show) -> updateGutter());
		highlightCurrentLine.addListener((obs, old, show) -> updateCurrentLine());
		fontFamily.addListener((obs, old, value) -> applyFont());
		fontSize.addListener((obs, old, value) -> applyFont());
		theme.addListener((obs, old, value) -> applyTheme());
		ThemeRegistry.getDefault().addWeakListener(themeListener);
		language.addListener((obs, old, value) -> applyLanguage());
		singleLine.addListener((obs, old, value) -> applySingleLine());
		updateGutter();
		updateCurrentLine();
		applyFont();
		applyTheme();

		findPanel.setOnClose(this::closeFindPanel);

		goToLinePanel.setOnClose(this::closeGoToLinePanel);

		foldState.revisionProperty().addListener((obs, old, value) -> foldMarkers.request());
		area.getVisibleParagraphs().addListener((ListChangeListener<Object>) change -> foldMarkers.request());
		area.layoutBoundsProperty().addListener((obs, old, value) -> foldMarkers.request());
		fontFamily.addListener((obs, old, value) -> foldMarkers.request());
		fontSize.addListener((obs, old, value) -> foldMarkers.request());
		addHighlightListener(foldMarkers::request);

		features.register(new CompletionFeature());
		features.register(new AutoIndentFeature());
		features.register(new AutoCloseFeature());
		features.register(new TabFeature());
		features.register(new BracketMatchFeature());
		features.register(new FindFeature());
		features.register(new GoToLineFeature());
		features.register(new LineEditFeature());
		features.register(new FoldFeature());
		features.register(new MultiCaretFeature());
		features.register(new DiagnosticsFeature());
		features.register(new DiagnosticHoverFeature());
		features.register(new FormatFeature());
		features.register(new SingleLineFeature());
	}

	public final String getText() {
		return area.getText();
	}

	public final void setText(String text) {
		extraCarets.clear();
		area.replaceText(normalize(text));
		area.getUndoManager().forgetHistory();
	}

	public final ObservableValue<String> textProperty() {
		return area.textProperty();
	}

	public final int getLength() {
		return area.getLength();
	}

	public final String getText(int start, int end) {
		return area.getText(start, end);
	}

	public final EditorLanguage getLanguageDefinition() {
		LanguageRegistry registry = LanguageRegistry.getDefault();
		return registry.find(getLanguage()).orElseGet(() -> registry.require(LanguageRegistry.PLAIN_TEXT));
	}

	public final List<IndexRange> getSelectionRanges() {
		List<IndexRange> ranges = new ArrayList<>();
		ranges.add(area.getSelection());
		ranges.addAll(extraCarets.ranges());
		return ranges;
	}

	public final void setSelectionRanges(IndexRange primary, List<IndexRange> extras) {
		int length = area.getLength();
		extraCarets.clear();
		area.selectRange(Math.min(primary.getStart(), length), Math.min(primary.getEnd(), length));
		for (IndexRange range : extras) {
			extraCarets.addRange(range.getStart(), range.getEnd());
		}
	}

	public final boolean hasMultiLineSelection() {
		for (IndexRange range : getSelectionRanges()) {
			if (range.getLength() > 0 && getLineOfOffset(range.getStart()) != getLineOfOffset(range.getEnd())) {
				return true;
			}
		}
		return false;
	}

	public final int getCaretPosition() {
		return area.getCaretPosition();
	}

	public final void applyEdits(List<TextEdit> edits) {
		if (edits.isEmpty()) {
			return;
		}
		int length = area.getLength();
		int previousEnd = 0;
		for (TextEdit edit : edits) {
			if (edit.start() < previousEnd || edit.end() > length) {
				throw new IllegalArgumentException("Edits must be ascending, non-overlapping and inside the document");
			}
			previousEnd = edit.end();
		}
		if (edits.size() == 1) {
			TextEdit edit = edits.get(0);
			area.replaceText(edit.start(), edit.end(), edit.text());
			return;
		}
		var builder = area.createMultiChange(edits.size());
		int shift = 0;
		for (TextEdit edit : edits) {
			builder.replaceTextAbsolutely(edit.start() + shift, edit.end() + shift, edit.text());
			shift += edit.text().length() - (edit.end() - edit.start());
		}
		builder.commit();
	}

	public final void setCaretPositions(int primary, List<Integer> extras) {
		area.moveTo(Math.max(0, Math.min(primary, area.getLength())));
		extraCarets.clear();
		for (int position : extras) {
			extraCarets.add(position);
		}
	}

	public final IndexRange getSelection() {
		return area.getSelection();
	}

	public final void moveCaret(int position) {
		area.moveTo(position);
	}

	public final void selectRange(int anchor, int caret) {
		area.selectRange(anchor, caret);
	}

	public final void replaceText(int start, int end, String text) {
		area.replaceText(start, end, normalize(text));
	}

	@FunctionalInterface
	public interface TextChangeListener {

		void onChange(int position, int removedLength, int insertedLength);
	}

	public final ObservableValue<Integer> caretPositionProperty() {
		return area.caretPositionProperty();
	}

	public final Collection<String> getStyleClassesAt(int offset) {
		return area.getStyleOfChar(offset);
	}

	public final void setStyleClasses(int start, int end, Collection<String> classes) {
		area.setStyle(start, end, classes);
	}

	public final Runnable addTextChangeListener(TextChangeListener listener) {
		Objects.requireNonNull(listener, "listener");
		Subscription subscription = area.plainTextChanges().subscribe(change -> listener.onChange(change.getPosition(),
				change.getRemoved().length(), change.getInserted().length()));
		return subscription::unsubscribe;
	}

	public final void addStyleClass(int start, int end, String styleClass) {
		changeStyleClass(start, end, styleClass, true);
	}

	public final void removeStyleClass(int start, int end, String styleClass) {
		changeStyleClass(start, end, styleClass, false);
	}

	public final Runnable addHighlightListener(Runnable listener) {
		Objects.requireNonNull(listener, "listener");
		return highlighter.addAppliedListener(listener);
	}

	public final boolean isTextAreaEvent(Event event) {
		if (!(event.getTarget() instanceof Node node)) {
			return false;
		}
		for (Node current = node; current != null; current = current.getParent()) {
			if (current == area) {
				return true;
			}
		}
		return false;
	}

	public final boolean addExtraCaret(int position) {
		return extraCarets.add(position);
	}

	public final void toggleExtraCaret(int position) {
		extraCarets.toggle(position);
	}

	public final void clearExtraCarets() {
		extraCarets.clear();
	}

	public final boolean hasExtraCarets() {
		return !extraCarets.isEmpty();
	}

	public final List<Integer> getCaretPositions() {
		List<Integer> positions = new ArrayList<>();
		positions.add(area.getCaretPosition());
		positions.addAll(extraCarets.positions());
		return positions;
	}

	public final int offsetAt(double screenX, double screenY) {
		Point2D point = area.screenToLocal(screenX, screenY);
		return area.hit(point.getX(), point.getY()).getInsertionIndex();
	}

	public final OptionalInt charIndexAt(double screenX, double screenY) {
		Point2D point = area.screenToLocal(screenX, screenY);
		return area.hit(point.getX(), point.getY()).getCharacterIndex();
	}


	public final Optional<Bounds> getCaretBoundsOnScreen() {
		return area.getCaretBounds();
	}

	public final Optional<Bounds> getCharacterBoundsOnScreen(int from, int to) {
		return area.getCharacterBoundsOnScreen(from, to);
	}

	public final CompletionProvider effectiveCompletionProvider() {
		CompletionProvider provider = getCompletionProvider();
		return provider != null ? provider : BuiltInCompletion.forLanguage(getLanguageDefinition());
	}

	public final boolean isFindPanelVisible() {
		return findPanel.isVisible();
	}

	public final void setFindPanelVisible(boolean visible) {
		findPanel.setVisible(visible);
	}

	public final BooleanProperty findPanelVisibleProperty() {
		return findPanel.visibleProperty();
	}

	public final void revealSelection() {
		area.requestFollowCaret();
	}

	public final int getLineCount() {
		return area.getParagraphs().size();
	}

	public final String getLineText(int line) {
		return area.getText(line);
	}

	public final int getLineStart(int line) {
		return area.getAbsolutePosition(line, 0);
	}

	public final int getLineOfOffset(int offset) {
		return area.offsetToPosition(offset, Bias.Forward).getMajor();
	}

	public final boolean isEditable() {
		return area.isEditable();
	}

	public final void setEditable(boolean editable) {
		area.setEditable(editable);
	}

	public final BooleanProperty editableProperty() {
		return area.editableProperty();
	}

	public final boolean isShowLineNumbers() {
		return showLineNumbers.get();
	}

	public final void setShowLineNumbers(boolean show) {
		showLineNumbers.set(show);
	}

	public final BooleanProperty showLineNumbersProperty() {
		return showLineNumbers;
	}

	public final boolean isHighlightCurrentLine() {
		return highlightCurrentLine.get();
	}

	public final void setHighlightCurrentLine(boolean highlight) {
		highlightCurrentLine.set(highlight);
	}

	public final BooleanProperty highlightCurrentLineProperty() {
		return highlightCurrentLine;
	}

	public final String getFontFamily() {
		return fontFamily.get();
	}

	public final void setFontFamily(String family) {
		fontFamily.set(family);
	}

	public final StringProperty fontFamilyProperty() {
		return fontFamily;
	}

	public final double getFontSize() {
		return fontSize.get();
	}

	public final void setFontSize(double size) {
		if (!(size > 0)) {
			throw new IllegalArgumentException("Font size must be greater than 0: " + size);
		}
		fontSize.set(size);
	}

	public final DoubleProperty fontSizeProperty() {
		return fontSize;
	}

	public final String getTheme() {
		return theme.get();
	}

	public final void setTheme(String id) {
		theme.set(ThemeRegistry.getDefault().require(id).getId());
	}

	public final void setTheme(EditorTheme value) {
		Objects.requireNonNull(value, "theme");
		ThemeRegistry.getDefault().register(value);
		theme.set(value.getId());
	}
	
	public final StringProperty themeProperty() {
		return theme;
	}

	public final String getLanguage() {
		return language.get();
	}

	public final void setLanguage(String id) {
		language.set(LanguageRegistry.getDefault().require(id).getId());
	}

	public final StringProperty languageProperty() {
		return language;
	}

	public final int getTabSize() {
		return tabSize.get();
	}

	public final void setTabSize(int size) {
		if (size < 1 || size > 16) {
			throw new IllegalArgumentException("Tab size must be between 1 and 16: " + size);
		}
		tabSize.set(size);
	}

	public final IntegerProperty tabSizeProperty() {
		return tabSize;
	}

	public final boolean isInsertSpaces() {
		return insertSpaces.get();
	}

	public final void setInsertSpaces(boolean spaces) {
		insertSpaces.set(spaces);
	}

	public final BooleanProperty insertSpacesProperty() {
		return insertSpaces;
	}
	
	public final boolean isSingleLine() {
		return singleLine.get();
	}

	public final void setSingleLine(boolean value) {
		singleLine.set(value);
	}

	public final BooleanProperty singleLineProperty() {
		return singleLine;
	}

	public final String getIndentUnit() {
		return isInsertSpaces() ? " ".repeat(getTabSize()) : "\t";
	}

	public final DiagnosticsProvider getDiagnosticsProvider() {
		return diagnosticsProvider.get();
	}

	public final CodeFormatter getFormatter() {
		return formatter.get();
	}

	public final void setFormatter(CodeFormatter value) {
		formatter.set(value);
	}

	public final CompletionProvider getCompletionProvider() {
		return completionProvider.get();
	}

	public final void setCompletionProvider(CompletionProvider provider) {
		completionProvider.set(provider);
	}

	public final boolean isCompletionAutoOpen() {
	    return completionAutoOpen.get();
	}

	public final void setCompletionAutoOpen(boolean value) {
	    completionAutoOpen.set(value);
	}

	public final BooleanProperty completionAutoOpenProperty() {
	    return completionAutoOpen;
	}

	public final int getCompletionMinChars() {
	    return completionMinChars.get();
	}

	public final void setCompletionMinChars(int chars) {
	    if (chars < 1 || chars > 10) {
	        throw new IllegalArgumentException("Completion min chars must be between 1 and 10: " + chars);
	    }
	    completionMinChars.set(chars);
	}

	public final IntegerProperty completionMinCharsProperty() {
	    return completionMinChars;
	}

	public final ReadOnlyBooleanProperty textFocusedProperty() {
	    return area.focusedProperty();
	}
	
	public final ObjectProperty<CompletionProvider> completionProviderProperty() {
		return completionProvider;
	}

	public final List<CompletionItem> getCompletions() {
		CompletionProvider provider = effectiveCompletionProvider();
		if (provider == null) {
			return List.of();
		}
		try {
			return List.copyOf(provider.complete(getText(), getCaretPosition()));
		} catch (RuntimeException ex) {
			LOG.log(System.Logger.Level.WARNING, "Completion provider failed", ex);
			return List.of();
		}
	}

	public final ObjectProperty<CodeFormatter> formatterProperty() {
		return formatter;
	}

	public final void setDiagnosticsProvider(DiagnosticsProvider provider) {
		diagnosticsProvider.set(provider);
	}

	public final ObjectProperty<DiagnosticsProvider> diagnosticsProviderProperty() {
		return diagnosticsProvider;
	}

	public final List<Diagnostic> getDiagnostics() {
		return diagnostics.get();
	}

	public final ReadOnlyObjectProperty<List<Diagnostic>> diagnosticsProperty() {
		return diagnostics.getReadOnlyProperty();
	}

	public final int getErrorCount() {
		return errorCount.get();
	}

	public final ReadOnlyIntegerProperty errorCountProperty() {
		return errorCount.getReadOnlyProperty();
	}

	public final void publishDiagnostics(List<Diagnostic> list) {
		List<Diagnostic> copy = List.copyOf(list);
		int errors = 0;
		for (Diagnostic diagnostic : copy) {
			if (diagnostic.severity() == DiagnosticSeverity.ERROR) {
				errors++;
			}
		}
		diagnostics.set(copy);
		errorCount.set(errors);
	}

	public final EditorFeatures features() {
		return features;
	}

	@Override
	public void requestFocus() {
		area.requestFocus();
	}

	@Override
	protected Skin<?> createDefaultSkin() {
		return new CodeEditorSkin(this);
	}

	@Override
	public String getUserAgentStylesheet() {
		String url = baseStylesheetUrl;
		if (url == null) {
			URL resource = Objects.requireNonNull(CodeEditor.class.getResource(BASE_STYLESHEET),
					"Missing resource " + BASE_STYLESHEET);
			url = resource.toExternalForm();
			baseStylesheetUrl = url;
		}
		return url;
	}

	CodeArea area() {
		return area;
	}

	private void closeFindPanel() {
		setFindPanelVisible(false);
		area.requestFocus();
	}


	public final boolean isGoToLineVisible() {
		return goToLinePanel.isVisible();
	}

	public final void setGoToLineVisible(boolean visible) {
		goToLinePanel.setVisible(visible);
	}

	public final void goToLine(int line, int column) {
		int index = Math.min(Math.max(line, 1), getLineCount()) - 1;
		int offset = GoToLineLogic.offset(getLineStart(index), getLineText(index).length(), column);
		area.moveTo(offset);
		area.showParagraphAtCenter(index);
		area.requestFocus();
	}

	public final boolean isLineHidden(int line) {
		return line >= 0 && line < area.getParagraphs().size() && area.isFolded(line);
	}

	public final void hideLines(int first, int last) {
		if (first < 1 || last < first || last >= area.getParagraphs().size()) {
			return;
		}
		int anchor = area.getAnchor();
		int caret = area.getCaretPosition();
		int hiddenStart = area.getAbsolutePosition(first, 0);
		int hiddenEnd = area.getAbsolutePosition(last, area.getParagraphLength(last));
		area.foldParagraphs(first - 1, last);
		if (isBetween(anchor, hiddenStart, hiddenEnd) || isBetween(caret, hiddenStart, hiddenEnd)) {
			area.moveTo(hiddenStart - 1);
		} else {
			area.selectRange(anchor, caret);
		}
	}

	public final void showLinesAfter(int header) {
		if (header < 0 || !isLineHidden(header + 1)) {
			return;
		}
		int anchor = area.getAnchor();
		int caret = area.getCaretPosition();
		area.unfoldParagraphs(header);
		area.selectRange(anchor, caret);
	}

	public final boolean toggleFold(int line) {
		if (!features.isEnabled(FoldFeature.ID)) {
			return false;
		}
		return features.find(FoldFeature.ID).filter(FoldFeature.class::isInstance)
				.map(feature -> ((FoldFeature) feature).toggleAt(line)).orElse(false);
	}

	public final boolean markFoldHeaders(String styleClass) {
		int count = area.getParagraphs().size();
		boolean any = false;
		for (int line = 0; line < count; line++) {
			boolean header = line + 1 < count && area.isFolded(line + 1) && !area.isFolded(line);
			Collection<String> style = area.getParagraph(line).getParagraphStyle();
			if (header != style.contains(styleClass)) {
				List<String> updated = new ArrayList<>(style);
				if (header) {
					updated.add(styleClass);
				} else {
					updated.remove(styleClass);
				}
				area.setParagraphStyle(line, updated);
			}
			any |= header;
		}
		return any;
	}

	public final BitSet offsetsWithStyle(Collection<String> classes) {
		BitSet result = new BitSet();
		int length = area.getLength();
		if (length == 0) {
			return result;
		}
		int offset = 0;
		for (StyleSpan<Collection<String>> span : area.getStyleSpans(0, length)) {
			int end = offset + span.getLength();
			if (span.getStyle() != null) {
				for (String styleClass : span.getStyle()) {
					if (classes.contains(styleClass)) {
						result.set(offset, end);
						break;
					}
				}
			}
			offset = end;
		}
		return result;
	}

	private static boolean isBetween(int offset, int start, int end) {
		return offset >= start && offset <= end;
	}

	private void closeGoToLinePanel() {
		setGoToLineVisible(false);
		area.requestFocus();
	}

	private void changeStyleClass(int start, int end, String styleClass, boolean add) {
		Objects.requireNonNull(styleClass, "styleClass");
		int from = Math.max(0, start);
		int to = Math.min(area.getLength(), end);
		if (to <= from) {
			return;
		}
		StyleSpans<Collection<String>> spans = area.getStyleSpans(from, to).mapStyles(styles -> {
			if (styles.contains(styleClass) == add) {
				return styles;
			}
			List<String> updated = new ArrayList<>(styles);
			if (add) {
				updated.add(styleClass);
			} else {
				updated.remove(styleClass);
			}
			return updated;
		});
		area.setStyleSpans(from, spans);
	}

	private void updateGutter() {
		area.setParagraphGraphicFactory(isShowLineNumbers()
				? new LineNumberGutter(area, foldState, this::isLineHidden, line -> toggleFold(line))
				: null);
	}

	private void updateCurrentLine() {
		if (isHighlightCurrentLine() && !isSingleLine()) {
			getStyleClass().remove(NO_CURRENT_LINE);
		} else if (!getStyleClass().contains(NO_CURRENT_LINE)) {
			getStyleClass().add(NO_CURRENT_LINE);
		}
	}

	private String normalize(String text) {
		String value = text == null ? "" : text;
		return isSingleLine() ? SingleLineText.strip(value) : value;
	}

	private void applySingleLine() {
		updateGutter();
		updateCurrentLine();
		if (isSingleLine()) {
			if (!getStyleClass().contains(SINGLE_LINE_CLASS)) {
				getStyleClass().add(SINGLE_LINE_CLASS);
			}
			for (String id : SINGLE_LINE_DISABLED) {
				if (features.isEnabled(id)) {
					features.setEnabled(id, false);
					disabledBySingleLine.add(id);
				}
			}
			String text = area.getText();
			String stripped = SingleLineText.strip(text);
			if (!stripped.equals(text)) {
				area.replaceText(stripped);
			}
			updateSingleLineHeight();
		} else {
			getStyleClass().remove(SINGLE_LINE_CLASS);
			for (String id : disabledBySingleLine) {
				features.setEnabled(id, true);
			}
			disabledBySingleLine.clear();
			setMinHeight(0);
			setPrefHeight(DEFAULT_PREF_HEIGHT);
			setMaxHeight(Region.USE_COMPUTED_SIZE);
		}
	}

	private void updateSingleLineHeight() {
		if (!isSingleLine()) {
			return;
		}
		Text probe = new Text("Ag");
		probe.setFont(Font.font(FontSupport.resolve(getFontFamily()), Math.max(1.0, getFontSize())));
		double height = Math.ceil(probe.getLayoutBounds().getHeight()) + 2 * SINGLE_LINE_PADDING;
		setMinHeight(height);
		setPrefHeight(height);
		setMaxHeight(height);
	}
	
	private void applyFont() {
		String family = FontSupport.resolve(getFontFamily());
		String css = FontSupport.MONOSPACE.equals(family) ? family : "'" + family + "'";
		double size = Math.max(1.0, getFontSize());
		area.setStyle("-fx-font-family: " + css + "; -fx-font-size: " + size + "px;");
		updateSingleLineHeight();
	}

	private void onThemeRegistered(EditorTheme registered) {
		if (!registered.getId().equals(getTheme())) {
			return;
		}
		if (Platform.isFxApplicationThread()) {
			applyTheme();
		} else {
			Platform.runLater(this::applyTheme);
		}
	}
	
	private void applyTheme() {
		Optional<EditorTheme> found = ThemeRegistry.getDefault().find(getTheme());
		if (found.isEmpty()) {
			LOG.log(System.Logger.Level.WARNING, "Unknown editor theme ''{0}'', keeping the current theme", getTheme());
			return;
		}
		List<String> sheets = found.get().getStylesheets();
		getStylesheets().removeAll(appliedStylesheets);
		getStylesheets().addAll(sheets);
		appliedStylesheets = sheets;
	}

	private void applyLanguage() {
		Optional<EditorLanguage> found = LanguageRegistry.getDefault().find(getLanguage());
		if (found.isEmpty()) {
			LOG.log(System.Logger.Level.WARNING, "Unknown editor language ''{0}'', keeping the current language",
					getLanguage());
			return;
		}
		highlighter.setTokenizer(found.get().getTokenizer());
	}
}