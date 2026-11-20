package io.github.itech_framework.code_editor;

import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpan;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.TwoDimensional.Bias;
import org.fxmisc.richtext.util.UndoUtils;
import org.reactfx.Subscription;

import io.github.itech_framework.code_editor.feature.AutoCloseFeature;
import io.github.itech_framework.code_editor.feature.AutoIndentFeature;
import io.github.itech_framework.code_editor.feature.BracketMatchFeature;
import io.github.itech_framework.code_editor.feature.FindFeature;
import io.github.itech_framework.code_editor.feature.FoldFeature;
import io.github.itech_framework.code_editor.feature.GoToLineFeature;
import io.github.itech_framework.code_editor.feature.LineEditFeature;
import io.github.itech_framework.code_editor.feature.TabFeature;
import io.github.itech_framework.code_editor.internal.FindPanel;
import io.github.itech_framework.code_editor.internal.FoldState;
import io.github.itech_framework.code_editor.internal.FontSupport;
import io.github.itech_framework.code_editor.internal.GoToLineLogic;
import io.github.itech_framework.code_editor.internal.GoToLinePanel;
import io.github.itech_framework.code_editor.internal.LineNumberGutter;
import io.github.itech_framework.code_editor.internal.SyntaxHighlighter;
import io.github.itech_framework.code_editor.language.EditorLanguage;
import io.github.itech_framework.code_editor.language.LanguageRegistry;
import io.github.itech_framework.code_editor.theme.EditorTheme;
import io.github.itech_framework.code_editor.theme.ThemeManager;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Skin;

public class CodeEditor extends Control {

    public static final String DEFAULT_FONT_FAMILY = "JetBrains Mono, Cascadia Code, Consolas, Menlo, DejaVu Sans Mono, monospace";

    public static final double DEFAULT_FONT_SIZE = 14;

    public static final int DEFAULT_TAB_SIZE = 4;

    private static final System.Logger LOG = System.getLogger(CodeEditor.class.getName());

    private static final String BASE_STYLESHEET = "css/base.css";

    private static final String NO_CURRENT_LINE = "no-current-line";

    private static volatile String baseStylesheetUrl;

    private final CodeArea area = new CodeArea();

    private final BooleanProperty showLineNumbers = new SimpleBooleanProperty(this, "showLineNumbers", true);

    private final BooleanProperty highlightCurrentLine = new SimpleBooleanProperty(this, "highlightCurrentLine", true);

    private final StringProperty fontFamily = new SimpleStringProperty(this, "fontFamily", DEFAULT_FONT_FAMILY);

    private final DoubleProperty fontSize = new SimpleDoubleProperty(this, "fontSize", DEFAULT_FONT_SIZE);

    private final StringProperty theme = new SimpleStringProperty(this, "theme", ThemeManager.DARK_PLUS);

    private final StringProperty language = new SimpleStringProperty(this, "language", LanguageRegistry.PLAIN_TEXT);

    private final IntegerProperty tabSize = new SimpleIntegerProperty(this, "tabSize", DEFAULT_TAB_SIZE);

    private final BooleanProperty insertSpaces = new SimpleBooleanProperty(this, "insertSpaces", true);

    private final EditorFeatures features = new EditorFeatures(this);
    
    private final FindPanel findPanel = new FindPanel();
    
    private final GoToLinePanel goToLinePanel = new GoToLinePanel();
    
    private final FoldState foldState = new FoldState();

    private String appliedStylesheet;

    private final SyntaxHighlighter highlighter;

    public CodeEditor() {
        getStyleClass().add("code-editor");
        setFocusTraversable(false);
        setMinSize(0, 0);
        setPrefSize(600, 400);
        area.setUndoManager(UndoUtils.plainTextUndoManager(area));
        highlighter = new SyntaxHighlighter(area, LanguageRegistry.getDefault().require(getLanguage()).getTokenizer());
        showLineNumbers.addListener((obs, old, show) -> updateGutter());
        highlightCurrentLine.addListener((obs, old, show) -> updateCurrentLine());
        fontFamily.addListener((obs, old, value) -> applyFont());
        fontSize.addListener((obs, old, value) -> applyFont());
        theme.addListener((obs, old, value) -> applyTheme());
        language.addListener((obs, old, value) -> applyLanguage());
        updateGutter();
        updateCurrentLine();
        applyFont();
        applyTheme();
        
        findPanel.setOnClose(this::closeFindPanel);
        
        goToLinePanel.setOnClose(this::closeGoToLinePanel);
        
        features.register(new AutoIndentFeature());
        features.register(new AutoCloseFeature());
        features.register(new TabFeature());
        features.register(new BracketMatchFeature());
        features.register(new FindFeature());
        features.register(new GoToLineFeature());
        features.register(new LineEditFeature());
        features.register(new FoldFeature());
    }

    public final String getText() {
        return area.getText();
    }

    public final void setText(String text) {
        area.replaceText(text == null ? "" : text);
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

    public final int getCaretPosition() {
        return area.getCaretPosition();
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
        area.replaceText(start, end, text == null ? "" : text);
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
        Subscription subscription = area.plainTextChanges().subscribe(change -> listener.onChange(
                change.getPosition(), change.getRemoved().length(), change.getInserted().length()));
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
        theme.set(ThemeManager.require(id).getId());
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

    public final String getIndentUnit() {
        return isInsertSpaces() ? " ".repeat(getTabSize()) : "\t";
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

    public final FindPanel findPanel() {
        return findPanel;
    }

    private void closeFindPanel() {
        setFindPanelVisible(false);
        area.requestFocus();
    }
    
    public final GoToLinePanel goToLinePanel() {
        return goToLinePanel;
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
    
    public final FoldState foldState() {
        return foldState;
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
        area.setParagraphGraphicFactory(isShowLineNumbers() ? new LineNumberGutter(area) : null);
    }

    private void updateCurrentLine() {
        if (isHighlightCurrentLine()) {
            getStyleClass().remove(NO_CURRENT_LINE);
        } else if (!getStyleClass().contains(NO_CURRENT_LINE)) {
            getStyleClass().add(NO_CURRENT_LINE);
        }
    }

    private void applyFont() {
        String family = FontSupport.resolve(getFontFamily());
        String css = FontSupport.MONOSPACE.equals(family) ? family : "'" + family + "'";
        double size = Math.max(1.0, getFontSize());
        area.setStyle("-fx-font-family: " + css + "; -fx-font-size: " + size + "px;");
    }

    private void applyTheme() {
        Optional<EditorTheme> found = ThemeManager.find(getTheme());
        if (found.isEmpty()) {
            LOG.log(System.Logger.Level.WARNING, "Unknown editor theme ''{0}'', keeping the current theme", getTheme());
            return;
        }
        String stylesheet = found.get().getStylesheet();
        if (appliedStylesheet != null) {
            getStylesheets().remove(appliedStylesheet);
        }
        getStylesheets().add(stylesheet);
        appliedStylesheet = stylesheet;
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