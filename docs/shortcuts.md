# Keyboard shortcuts

Ctrl/Cmd means the Ctrl key on Windows and Linux, and the Cmd key on macOS. A plain Ctrl means the Control key on every platform. Each section names the feature id, so you can turn a group off with `editor.features().setEnabled(id, false)`.

Everything here has been checked with a US keyboard layout on Windows. The macOS keys follow the code but have not been tried on a Mac.

## Find and replace

Feature id: `find`

| Keys | What happens |
|---|---|
| Ctrl/Cmd+F | Opens the find panel. A short single-line selection is copied into the search field. |
| Ctrl/Cmd+H | Opens the panel with the replace row. Does nothing extra in a read-only editor. |
| F3, Shift+F3 | Goes to the next or previous match. Opens the panel if it is closed. |
| Esc | Closes the panel when it is open. |

Inside the find field:

| Keys | What happens |
|---|---|
| Enter or F3 | Next match |
| Shift+Enter or Shift+F3 | Previous match |
| Esc | Close the panel |

Inside the replace field:

| Keys | What happens |
|---|---|
| Enter | Replace the current match |
| Ctrl+Alt+Enter | Replace all matches in one step, so one undo reverts it |
| Esc | Close the panel |

The panel has toggles for case, whole word and regular expressions. In regex mode the replacement can use `$1`. In plain mode it is taken literally.

Cmd+H hides the application on macOS, so it may never reach the editor there.

## Go to line

Feature id: `go-to-line`

Press Ctrl+G (the Control key, also on macOS), type a line number or `line:column`, then press Enter. Esc closes the box. A line outside the document or a column of 0 shows an error message instead of moving the caret.

## Line editing

Feature id: `line-edit`. These keys only work when the editor is editable.

| Keys | What happens |
|---|---|
| Alt+Up, Alt+Down | Move the current line, or all selected lines, up or down |
| Alt+Shift+Up, Alt+Shift+Down | Duplicate the line or selected lines above or below |
| Ctrl/Cmd+Shift+K | Delete the line or selected lines |
| Ctrl/Cmd+/ | Toggle a line comment |

Toggling a comment needs a `lineComment` value in the language template. Without one, nothing happens. Plain text has none.

The comment shortcut also accepts the slash key by its key code and Shift+7, so layouts where `/` needs Shift usually work. A side effect is that Ctrl/Cmd+Shift+7 also toggles a comment on a US layout.

## Folding

Feature id: `fold`

| Keys | What happens |
|---|---|
| Ctrl/Cmd+Shift+[ | Fold the block around the caret |
| Ctrl/Cmd+Shift+] | Unfold the block at the caret |

You can also click the marker in the gutter. Blocks made with `{}` and `[]` can fold when they span at least three lines. Folding is switched off for documents above 2,000,000 characters.

On layouts where `[` and `]` need AltGr or another modifier, these two keys may not work. Use the gutter markers there.

## Multiple carets

Feature id: `multi-caret`

| Keys | What happens |
|---|---|
| Alt+click | Add a caret at the click, or remove the one that is there |
| Ctrl/Cmd+Alt+Up, Ctrl/Cmd+Alt+Down | Add a caret on the line above or below |
| Ctrl/Cmd+D | Select the next occurrence of the selected text. With nothing selected, it selects the word at the caret first. |
| Ctrl/Cmd+Shift+L | Select every occurrence |
| Alt+Shift+I | Put a caret at the end of each selected line |
| Esc | Remove the extra carets. Does nothing while the find or go-to-line panel is open. |

Typing, Backspace, Delete, Enter, Tab and Shift+Tab work at every caret, and one Ctrl+Z undoes the whole edit. After an edit, any selections collapse to carets.

There can be at most 200 extra carets. The occurrence search is case-sensitive. It matches whole words only when it started from the word at the caret.

Arrow keys, Home, End and most other non-typing keys remove the extra carets. A plain mouse click does too. Moving all carets together is not built.

## Indenting

Feature ids: `tab-indent` and `auto-indent`

| Keys | What happens |
|---|---|
| Tab | Insert one indent. With a multi-line selection, indent every selected line. |
| Shift+Tab | Remove one indent from the line or from every selected line |
| Enter | Start the new line at the indent of the current one, and add a level after a line that ends with `(`, `[` or `{` |

The indent is spaces or a tab character, depending on the `insertSpaces` property, and its width comes from `tabSize` (1 to 16). Auto-indent skips Enter when Shift, Ctrl, Alt or Cmd is held, so Shift+Enter always gives a plain new line.

With extra carets, Tab and Enter are handled by the multi-caret code instead.

## Brackets and quotes

Feature id: `auto-close`

- Typing an opening bracket or quote inserts the closing one and puts the caret between them.
- Typing the closing character when it is already next to the caret moves past it.
- With text selected, typing an opening character wraps the selection.
- Backspace between an empty pair removes both characters.

The pairs come from the `autoClosingPairs` entry of the language template. A template can turn auto-close off by setting it to an empty list. It is off while extra carets exist.

Feature id: `bracket-match`. The bracket next to the caret and its partner are highlighted for `()`, `[]` and `{}`. It has no keys. Brackets inside strings and comments are ignored.

## Completion

Feature id: `completion`

| Keys | What happens |
|---|---|
| Ctrl+Space | Open the popup. Uses the Control key on every platform. |
| Up, Down | Move through the list |
| Enter or Tab | Insert the selected item |
| Esc | Close the popup. It stays closed until you start another word. |

The popup also opens by itself while you type word characters, once the word has `completionMinChars` characters (2 by default), or right after a character that the completion provider lists as a trigger. Set `completionAutoOpen` to false to turn that off. Accepting an item is one edit, so one undo reverts it.

It works with one caret and no selection. It stays closed in a read-only editor.

Some input method editors use Ctrl+Space to switch languages. If the key never reaches the editor, automatic opening still works.

## Formatting

Feature id: `format`

Press Shift+Alt+F to format the whole document. It only runs in an editable editor. The editor uses the formatter set with `setFormatter`, or the one named in the language template. Without either, nothing happens. The change is one edit, so one Ctrl+Z reverts it.

## Mouse

| Action | What happens |
|---|---|
| Alt+click | Add or remove a caret |
| Click a gutter marker | Fold or unfold that block |
| Rest the pointer on an underline | After about 0.4 seconds, a small box shows the message. It disappears when you move, type, scroll or click. |
| Plain click | Removes extra carets |

The hover box belongs to the `diagnostic-hover` feature. The underlines themselves come from `diagnostics`.

## Other keys

The editor keeps the default keys of RichTextFX for undo, redo, clipboard, select all and cursor movement. They are not changed here.