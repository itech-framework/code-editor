# Architecture and limits

## Overview

`CodeEditor` is one JavaFX `Control`. Inside, its skin stacks a RichTextFX `CodeArea` with a few overlays: the find panel, the go-to-line panel, the diagnostic hover box and the completion popup. The library needs only JavaFX and RichTextFX.

The text, the undo history and the carets belong to RichTextFX. Colors are style classes on ranges of text, such as `tok-keyword`. A block that is folded is hidden with a paragraph style, and its text stays in the document. So `getText()`, copy and search still see folded lines, and folding never adds an undo step.

Every editing behavior is a feature with an id, and the logic that doesn't need JavaFX lives in separate classes so it can be tested without a display.

## Packages

| Package | What's in it |
|---|---|
| `code_editor` | `CodeEditor`, its skin, `TextEdit`, and the feature registry (`EditorFeature`, `EditorFeatures`). |
| `completion`, `diagnostics`, `formatting` | The interfaces and records for the three plug-in points. |
| `language`, `language.template` | Languages, the tokenizer interface, and the JSON template engine. |
| `theme` | Themes, the JSON theme loader and the stylesheet generator. |
| `feature` | The built-in features. You switch them on and off by id, as described in [extending.md](extending.md). |
| `internal` | The panels, the popup, the gutter, the highlighter and the pure logic classes. Not public API. |
| `internal.json` | A small strict JSON reader used by the template loader, the theme loader, the JSON formatter and JSON diagnostics. |

The classes in `internal` that have no JavaFX in them, the ones whose names end in `Logic` for example, are the ones with unit tests. The panels, features and skin are checked in the demo app.

## How highlighting works

The highlighter remembers, for every line, the tokenizer state at its start and at its end.

When you edit, the entry for the changed line is dropped, and entries for added or removed lines are inserted or deleted to match. Then the highlighter waits until you pause for 60 ms. After that it starts one background pass at the first line it has no entry for, using the previous line's saved end state.

The pass stops as soon as a line after the edit starts in the same state it had before. That usually happens within a line or two. If the state hasn't settled after 500 lines past the last changed line, the pass ends there and another one is scheduled to carry on. Typing in the middle of a large file therefore re-colors only a few lines, and opening a block comment at the top re-colors until the comment's end.

The result goes back to the JavaFX thread and is applied in one call. If the text changed again in the meantime, the result is dropped.

Some changes restart from scratch: `setText`, a language switch, and any change that touches several places at once, such as a multi-caret edit.

Lines longer than 20,000 characters are not tokenized by the highlighter. A template language gives up earlier, at 10,000.

## Threads

Work that could be slow runs on a background thread. Each kind of work has one daemon thread that is shared by every `CodeEditor` in the JVM. If one editor's provider is slow, other editors wait behind it for that kind of work.

| Thread | Work | Starts |
|---|---|---|
| `code-editor-highlighter` | Coloring | 60 ms after the last change |
| `code-editor-fold` | Finding foldable blocks | 250 ms after the last change |
| `code-editor-find` | Searching the document | 250 ms after the last keystroke in the search field |
| `code-editor-diagnostics` | Your diagnostics provider, or the built-in one | 400 ms after the last change |
| `code-editor-completion` | Your completion provider, or the built-in one | When the popup is requested or refreshed |
| `code-editor-formatter` | Your formatter, or the built-in one | When the user presses Shift+Alt+F |

Each of these takes a copy of the text with `getText()` when it starts. Work goes to the background with that copy and comes back through `Platform.runLater`. Every request carries a counter, and a result is applied only if the counter hasn't moved on since. That is why a slow answer for old text never overwrites the screen.

A few small things run on the JavaFX thread with timers: bracket matching after 200 ms, the hover box after 400 ms, and fold markers after 30 ms.

Editor state is only ever changed on the JavaFX thread. A provider that touches the editor from its own thread is a bug.

## Themes and styles

`base.css` is the control's user agent stylesheet. It holds the layout and a light color default for each of the 55 color tokens. A theme adds its stylesheets on top. See [themes.md](themes.md).

## Limits

These limits keep the editor responsive on large files. Past a limit, the feature does less or nothing. It doesn't fail.

| Area | Limit | What happens past it |
|---|---|---|
| Highlighting | Lines over 20,000 characters | The line gets no colors. |
| Template languages | Lines over 10,000 characters, or 50 ms per line, or a state stack deeper than 64 | The line gets no colors, and the state carries on unchanged. |
| Folding | Documents over 2,000,000 characters | Folding is off. |
| Folding | Blocks shorter than 3 lines, or nested deeper than 256 | No marker for them. |
| Bracket matching | 20,000 characters each side of the caret | No match is shown if the partner is farther away. |
| Find | 1,000 matches, or 300 ms of searching | The search stops. At the match limit the count reads `1000+`. |
| Autocomplete, built-in | 50 suggestions. Document words need at least 3 characters. Only 200,000 characters around the caret are scanned. | Fewer suggestions. |
| Autocomplete, any provider | 200 items | The rest are dropped. |
| Diagnostics | 200 shown | Errors are kept first, then warnings, then info, then by position. |
| Lexical diagnostics | Documents over 1,000,000 characters, or 1,000 findings | No diagnostics, or the first 1,000. |
| JSON diagnostics and JSON formatter | Nesting deeper than 64 levels | Diagnostics report it as an error. The formatter leaves the text alone. |
| Brace formatter | Documents over 1,000,000 characters, or any line over 10,000 | Nothing is formatted. Indentation is capped at 64 levels. |
| Multiple carets | 200 | Adding more is ignored. |

## Known gaps

- Shortcuts have been tried on the US keyboard layout only. Keys that need a modifier for the symbol, such as Ctrl+/ and Ctrl+Shift+[, may differ elsewhere. Ctrl+/ also matches the Shift+7 chord that some layouts produce.
- Ctrl+H is the replace shortcut, and on macOS it is also the system's hide command. That hasn't been tried. Ctrl+Space may be taken by the operating system or an input method on some setups.
- Arrow keys clear extra carets instead of moving them. There is no column selection.
- Folding handles `{}` and `[]` only, not indentation. Unfolding an outer block also unfolds the blocks inside it.
- In space mode, a tab stop counts every character as one column, so lines with combining marks, as in Burmese, line up only roughly.
- Built-in autocomplete offers the language's keywords and words that are already in the document. It can't know the methods of a variable. For that, plug in a provider.
- Only `json`, `javascript` and `plaintext` are registered by default. `java.json` ships in the jar but you have to register it. Other languages come from templates you write.
- JavaScript gets lexical diagnostics only, for unclosed brackets, strings and comments. It isn't parsed.
- The library has been built and tested with Java 17, JavaFX 17 and RichTextFX 0.11.5, on Ubuntu and Windows. macOS, Java 21 and newer JavaFX versions haven't been tried. JavaFX 25 and later need RichTextFX 0.11.7 or newer.