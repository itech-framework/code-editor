# Extending the editor

The code has no Javadoc, so this page is the reference for the public API. Everything in the packages below is public. Anything in an `internal` package is not, and it can change between versions.

| Package | What's in it |
|---|---|
| `io.github.itech_framework.code_editor` | `CodeEditor`, `TextEdit`, `EditorFeature`, `EditorFeatures` |
| `...completion` | `CompletionProvider`, `CompletionItem` |
| `...diagnostics` | `DiagnosticsProvider`, `Diagnostic`, `DiagnosticSeverity` |
| `...formatting` | `CodeFormatter`, `FormatOptions` |
| `...language` | `LanguageRegistry`, `EditorLanguage`, `Tokenizer`, `Token`, `LineTokens`, `TokenizerState`, `BracketPair`, `FormatterSpec` |
| `...language.template` | `TemplateLoader`, `TemplateLanguages`, `TemplateException` |
| `...theme` | `ThemeRegistry`, `EditorTheme`, `ThemeLoader`, `ThemeException`, `ThemeTokens` |

Language templates are covered in [languages.md](languages.md) and themes in [themes.md](themes.md).

## Threads

Call `CodeEditor` methods and read its properties on the JavaFX thread.

Completion providers, diagnostics providers and formatters run on a background thread. They receive a copy of the text as a `String`. They must be safe to call from another thread, and they must not touch the editor or any JavaFX node. If the text changes while a provider is working, its result is thrown away. If a provider throws a `RuntimeException`, the editor logs it and carries on as if there were no result.

Offsets are positions in the Java string, counted in `char` units.

## Reading and changing text

| Method | What it does |
|---|---|
| `getText()`, `getText(start, end)`, `getLength()` | Read the text. |
| `setText(text)` | Replaces everything. It clears the undo history and any extra carets. `null` means empty. |
| `replaceText(start, end, text)` | One edit, one undo step. |
| `applyEdits(List<TextEdit>)` | Several edits as one undo step. The edits must be in ascending order, must not overlap, and must lie inside the text. Otherwise it throws `IllegalArgumentException`. |
| `getCaretPosition()`, `moveCaret(position)` | The main caret. |
| `getSelection()`, `selectRange(anchor, caret)` | The main selection. |
| `getSelectionRanges()`, `setSelectionRanges(primary, extras)` | The main selection plus any extra ones. |
| `getCaretPositions()`, `setCaretPositions(primary, extras)` | The main caret plus any extra ones. |
| `addExtraCaret(position)`, `toggleExtraCaret(position)`, `clearExtraCarets()`, `hasExtraCarets()` | Extra carets. |
| `getLineCount()`, `getLineText(i)`, `getLineStart(i)`, `getLineOfOffset(offset)` | Lines, counted from 0. |
| `goToLine(line, column)` | Moves the caret and scrolls. Both numbers start at 1 and are clamped to the document. |
| `revealSelection()` | Scrolls so the selection is visible. |
| `getCaretBoundsOnScreen()`, `getCharacterBoundsOnScreen(from, to)` | Screen rectangles, or empty if the text is scrolled out of view. |
| `offsetAt(screenX, screenY)`, `charIndexAt(screenX, screenY)` | Turn a screen position into a text offset. |

`TextEdit` is a record of `start`, `end` and the replacement `text`. Its constructor rejects a negative start and an end before the start.

## Settings

Every setting has a getter, a setter and a `...Property()` method.

| Property | Default | Notes |
|---|---|---|
| `editable` | `true` | |
| `showLineNumbers` | `true` | |
| `highlightCurrentLine` | `true` | |
| `fontFamily` | JetBrains Mono, Cascadia Code, Consolas, Menlo, DejaVu Sans Mono, monospace | A comma-separated list. The first one installed is used. |
| `fontSize` | `14` | Must be above 0. |
| `theme` | `dark-plus` | An id. `setTheme` also accepts an `EditorTheme`. An unknown id throws `IllegalArgumentException`. |
| `language` | `plaintext` | An id. An unknown id throws `IllegalArgumentException`. |
| `tabSize` | `4` | 1 to 16. |
| `insertSpaces` | `true` | `false` inserts a tab character. |
| `completionAutoOpen` | `true` | Open the popup while typing. |
| `completionMinChars` | `2` | 1 to 10. Characters typed before the popup opens by itself. |
| `findPanelVisible` | | Shows or hides the find panel. |
| `completionProvider`, `diagnosticsProvider`, `formatter` | `null` | `null` means the built-in behavior for the current language. |

Read-only: `textProperty()`, `caretPositionProperty()`, `textFocusedProperty()`, `diagnosticsProperty()` and `errorCountProperty()`.

## Features

Every editing behavior is a feature with an id. The built-in ids are `auto-indent`, `auto-close`, `tab-indent`, `bracket-match`, `find`, `go-to-line`, `line-edit`, `fold`, `multi-caret`, `completion`, `format`, `diagnostics` and `diagnostic-hover`.

```java
EditorFeatures features = editor.features();
features.setEnabled("fold", false);
features.isEnabled("fold");
features.ids();
```

An unknown id throws `IllegalArgumentException` and lists the registered ones. Ids are case-insensitive.

### Writing your own

Implement `EditorFeature` and register it. `register` installs it straight away. If a feature with the same id already exists, it is uninstalled and replaced.

This one upper-cases the selection on Ctrl+Shift+U:

```java
public final class UpperCaseFeature implements EditorFeature {

    private final EventHandler<KeyEvent> handler = this::onKey;

    private CodeEditor editor;

    @Override
    public String getId() {
        return "upper-case";
    }

    @Override
    public void install(CodeEditor target) {
        editor = target;
        target.addEventFilter(KeyEvent.KEY_PRESSED, handler);
    }

    @Override
    public void uninstall(CodeEditor target) {
        target.removeEventFilter(KeyEvent.KEY_PRESSED, handler);
        editor = null;
    }

    private void onKey(KeyEvent event) {
        if (event.isConsumed() || !editor.isTextAreaEvent(event) || !editor.isEditable()) {
            return;
        }
        if (event.getCode() != KeyCode.U || !event.isShortcutDown() || !event.isShiftDown()) {
            return;
        }
        IndexRange selection = editor.getSelection();
        if (selection.getLength() == 0) {
            return;
        }
        event.consume();
        String upper = editor.getText(selection.getStart(), selection.getEnd()).toUpperCase(Locale.ROOT);
        editor.replaceText(selection.getStart(), selection.getEnd(), upper);
        editor.selectRange(selection.getStart(), selection.getStart() + upper.length());
    }
}
```

```java
editor.features().register(new UpperCaseFeature());
```

Three things to get right in a key handler:

- Start with `isTextAreaEvent(event)`. Without it, your handler also fires while the user types in the find box, and it edits the document.
- Check `isConsumed()`. Handlers on the same node all run even after one of them consumes the event, in the order they were registered.
- Skip your edit when `hasExtraCarets()` is true unless you handle every caret. The built-in text features step aside in that case.

### Hooks for features

| Method | What it gives you |
|---|---|
| `addTextChangeListener(listener)` | Called with `(position, removedLength, insertedLength)` for every change to the text. |
| `caretPositionProperty()` | The caret offset. |
| `addHighlightListener(runnable)` | Runs each time syntax highlighting has been applied. |
| `getStyleClassesAt(offset)` | The style classes on one character. |
| `addStyleClass(start, end, cls)`, `removeStyleClass(start, end, cls)`, `setStyleClasses(start, end, classes)` | Put your own classes on a range of text. |

Both listener methods return a `Runnable`. Call it to unsubscribe. A feature should do that in `uninstall`.

The highlighter rewrites the style classes of any line it re-tokenizes, and that wipes classes you added there. Bracket matching, find and diagnostics all handle this by applying their classes again from a highlight listener, and yours should too. To color a class, add a rule to a stylesheet on the editor:

```css
.code-editor .code-area .text.my-class {
    -rtfx-background-color: rgba(255, 200, 0, 0.3);
}
```

## Completion

```java
public interface CompletionProvider {
    List<CompletionItem> complete(String text, int caret);

    default String triggerCharacters() {
        return "";
    }
}
```

`CompletionItem` has a `label` (what the list shows), an `insertText`, a `kind` (`KEYWORD`, `WORD` or `VARIABLE`), and the range `start` to `end` of the text it replaces. Normally `end` is the caret and `start` is where the word begins. `CompletionItem.of(text, kind, start, end)` is a shortcut for items whose label and inserted text are the same.

The editor calls the provider when the user presses Ctrl+Space, and while typing once a word reaches `completionMinChars`. It also calls it right after a typed character that appears in `triggerCharacters()`. A trigger character opens the popup even inside a string or a comment, where automatic opening is normally skipped.

The list shows items in the order you return them, up to 200, with the first one selected. The popup stays closed in a read-only editor, with a selection, or with extra carets.

This provider offers variable names after `{{`:

```java
public final class VariableCompletion implements CompletionProvider {

    private final List<String> names = List.of("baseUrl", "token", "userId");

    @Override
    public List<CompletionItem> complete(String text, int caret) {
        int start = caret;
        while (start > 0 && (Character.isLetterOrDigit(text.charAt(start - 1)) || text.charAt(start - 1) == '_')) {
            start--;
        }
        if (start < 2 || !text.startsWith("{{", start - 2)) {
            return List.of();
        }
        String prefix = text.substring(start, caret);
        List<CompletionItem> items = new ArrayList<>();
        for (String name : names) {
            if (name.startsWith(prefix)) {
                items.add(new CompletionItem(name, name + "}}", CompletionItem.Kind.VARIABLE, start, caret));
            }
        }
        return items;
    }

    @Override
    public String triggerCharacters() {
        return "{";
    }
}
```

```java
editor.setCompletionProvider(new VariableCompletion());
```

Setting a provider replaces the built-in one, so keywords and document words stop appearing. Set `null` to go back. To keep them, grab the built-in provider first and combine the two:

```java
CompletionProvider builtIn = editor.effectiveCompletionProvider();
CompletionProvider variables = new VariableCompletion();
editor.setCompletionProvider(new CompletionProvider() {
    @Override
    public List<CompletionItem> complete(String text, int caret) {
        List<CompletionItem> items = new ArrayList<>(variables.complete(text, caret));
        items.addAll(builtIn.complete(text, caret));
        return items;
    }

    @Override
    public String triggerCharacters() {
        return variables.triggerCharacters();
    }
});
```

`effectiveCompletionProvider()` returns the built-in provider for the language that is selected at that moment, so the combined provider keeps that language's keywords even if the language changes later.

`editor.getCompletions()` runs the provider for the current caret on the calling thread and returns the list. It's handy in tests.

## Diagnostics

```java
public interface DiagnosticsProvider {
    List<Diagnostic> diagnose(String text);
}
```

A `Diagnostic` is a record of `start`, `end`, a `DiagnosticSeverity` (`ERROR`, `WARNING` or `INFO`) and a `message`. The editor underlines the range with a wavy line, and the hover feature shows the message after the pointer rests on it for a moment.

```java
Pattern todo = Pattern.compile("TODO");
editor.setDiagnosticsProvider(text -> {
    List<Diagnostic> found = new ArrayList<>();
    Matcher matcher = todo.matcher(text);
    while (matcher.find()) {
        found.add(new Diagnostic(matcher.start(), matcher.end(), DiagnosticSeverity.INFO, "Unfinished work"));
    }
    return found;
});
```

How it behaves:

- The provider runs 400 ms after the user stops typing, and again when the language or the provider changes.
- While the user types, existing underlines move with the text, so they don't jump around before the next run.
- A range past the end of the text is cut to fit. An empty range is drawn over one character.
- At most 200 diagnostics are kept. If you return more, errors are kept before warnings and warnings before info, then the earliest in the text.
- `diagnosticsProperty()` holds the current list and `errorCountProperty()` counts the errors, so you can show a status line.

With no provider set, the built-in one depends on the language. The `json` language gets a strict JSON check that reports the first error. Every other language made from a template gets lexical checks: unterminated strings and comments, and unclosed or unmatched `()`, `[]` and `{}`. Both skip very large documents. Setting your own provider replaces the built-in one, and `null` brings it back.

## Formatting

```java
public interface CodeFormatter {
    List<TextEdit> format(String text, FormatOptions options);
}
```

`FormatOptions` gives you `tabSize()`, `insertSpaces()` and `indentUnit()`, which is the string for one level of indent. Return the edits that turn `text` into the formatted text, with positions in the text you were given. Return an empty list when nothing needs to change.

```java
Pattern trailing = Pattern.compile("[ \\t]+(?=\\r?\\n|$)");
editor.setFormatter((text, options) -> {
    List<TextEdit> edits = new ArrayList<>();
    Matcher matcher = trailing.matcher(text);
    while (matcher.find()) {
        edits.add(new TextEdit(matcher.start(), matcher.end(), ""));
    }
    return edits;
});
```

The user runs it with Shift+Alt+F in an editable editor. The edits apply as one undo step, and the selection is moved to the same place in the new text. If the text changed while the formatter was working, the result is dropped. Edits that overlap, run out of order or fall outside the text are rejected and logged.

With no formatter set, the editor uses the one named in the language template, and does nothing if there isn't one.

## A language written in code

If a template can't describe your language, implement `Tokenizer`. It colors one line at a time and passes a state from each line to the next:

```java
final class HashCommentTokenizer implements Tokenizer {

    private enum State implements TokenizerState {
        ROOT
    }

    @Override
    public TokenizerState initialState() {
        return State.ROOT;
    }

    @Override
    public LineTokens tokenize(String line, TokenizerState state) {
        int hash = line.indexOf('#');
        if (hash < 0) {
            return new LineTokens(List.of(), State.ROOT);
        }
        return new LineTokens(List.of(new Token(hash, line.length(), "comment.line")), State.ROOT);
    }
}
```

```java
LanguageRegistry.getDefault().register(EditorLanguage.builder("hashy")
        .displayName("Hashy")
        .extension("hashy")
        .lineComment("#")
        .tokenizer(new HashCommentTokenizer())
        .build());
```

A `Token` has start and end offsets inside the line and a scope, as described in [languages.md](languages.md). The line has no line break. Use an enum or a record for your state types. The highlighter, the brace formatter and the lexical diagnostics compare states with `equals`. The highlighter uses it to stop re-coloring once a line starts in the same state as before. If your states never equal each other, every edit re-colors the rest of the document.

The builder also takes `autoClosingPairs`, `formatter` (a `FormatterSpec`) and `keywords`. The keywords feed the completion list.