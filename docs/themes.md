# Themes

A theme is a set of colors. The editor ships with six: `dark-plus` (the default), `light-plus`, `monokai`, `github-light`, `solarized-dark` and `solarized-light`.

```java
editor.setTheme("monokai");
```

`setTheme` throws `IllegalArgumentException` for an id that isn't registered, and the message lists the ones that are. The `theme` property holds the id, so you can bind to it. `ThemeRegistry.getDefault().all()` returns every theme sorted by display name, which is handy for a dropdown.

## How a theme is applied

Every color in the editor is a CSS variable named `-ce-something`, defined on the `.code-editor` node. The library's `base.css` gives all 55 of them a light default. `dark-plus.css` and `light-plus.css` override them, and the other four built-in themes are small JSON files on top of one of those two.

When you switch themes, the editor takes the old theme's stylesheets out of its own `getStylesheets()` list and puts the new ones in. A theme you write yourself is a base theme plus a short generated stylesheet that overrides only the tokens you name. That stylesheet lives in memory as a `data:` URI, so nothing is written to disk. It needs a JavaFX version that accepts `data:` stylesheets, and 17.0.1 does.

The practical result is that a theme file only lists what differs from its parent.

## Writing a theme file

```json
{
  "version": 1,
  "id": "night-owl",
  "name": "Night Owl",
  "dark": true,
  "extends": "dark-plus",
  "colors": {
    "background": "#011627",
    "foreground": "#d6deeb",
    "token-keyword": "#c792ea",
    "token-string": "#ecc48d"
  }
}
```

| Field | Required | Meaning |
|---|---|---|
| `version` | yes | The integer `1`. |
| `id` | yes | The name you pass to `setTheme`. Trimmed and lowercased. |
| `name` | no | Display name. Defaults to the id. |
| `dark` | no | `true` or `false`. Defaults to `false`. It only picks the default parent. |
| `extends` | no | The id of a registered theme to build on. Defaults to `dark-plus` for a dark theme and `light-plus` otherwise. A theme can't extend itself. |
| `colors` | yes | An object of token names and colors. It can be empty, and then the theme is a copy of its parent. |

Unknown fields are errors. The token names in `colors` are the CSS variable names without the `-ce-` prefix, and the full list is below.

Colors can be written as `#rgb`, `#rrggbb`, `#rrggbbaa`, `rgb(r, g, b)` or `rgba(r, g, b, a)`. The channels are whole numbers from 0 to 255, and alpha is a decimal from 0 to 1. In `#rrggbbaa` the alpha is the last pair of digits.

`extends` has to name a theme that is already registered. That can be a built-in or one you loaded earlier, so themes can build on each other.

## Loading a theme

```java
EditorTheme theme = ThemeLoader.load(Path.of("night-owl.json"));
editor.setTheme(theme);
```

`setTheme(EditorTheme)` registers the theme in the default registry and selects it. `ThemeLoader.load(Path)` throws `IOException` if the file can't be read and `ThemeException` (unchecked) if the content is wrong. To load from a string you already have, use `ThemeLoader.load(source, json)`. The source is only a label that appears in error messages.

If you register a theme whose id already exists, it replaces the old one, and every editor using that id picks up the change straight away. That makes it easy to edit a file and reload it while you work on a theme.

## Building a theme in code

```java
EditorTheme theme = EditorTheme.builder("night-owl", "Night Owl", true)
        .extending("dark-plus")
        .color("background", "#011627")
        .color("token-keyword", "#c792ea")
        .build();
editor.setTheme(theme);
```

`color` accepts a token name with or without the `-ce-` prefix. A wrong token name or a bad color throws `IllegalArgumentException` with the same wording as the file loader. `build()` looks up the parent in the default registry, and `build(registry)` uses another one. If you never call `color`, the theme just reuses its parent's stylesheets.

`CodeEditor` always reads themes from the default registry. A separate `new ThemeRegistry()` is useful for tests and tools, but an editor won't see themes registered only there.

## The tokens

There are 55. `ThemeTokens.names()` returns the same list at run time.

| Group | Tokens | What they color |
|---|---|---|
| Editor | `background`, `foreground` | The editor background and the default text color |
| | `selection`, `caret` | Selected text and the caret |
| | `current-line` | The highlight behind the line with the caret |
| | `line-number`, `line-number-active` | Gutter numbers. The active one is for the caret line, and it also colors a fold arrow when the pointer is over it. |
| | `scroll-thumb`, `scroll-thumb-hover` | The scroll bar handle, normal and under the pointer |
| Syntax | `token-keyword`, `token-keyword-control`, `token-string`, `token-string-escape`, `token-number`, `token-comment`, `token-type`, `token-function`, `token-variable`, `token-constant`, `token-operator`, `token-punctuation`, `token-regexp`, `token-tag`, `token-attribute`, `token-invalid` | Text whose scope starts with that name. See [language templates](languages.md). |
| Brackets | `bracket-match`, `bracket-match-border` | Fill and outline of the matching bracket pair |
| Find and go to line | `find-panel-background`, `find-panel-border`, `find-foreground` | The panel and its text |
| | `find-field-background`, `find-field-border`, `find-field-border-focused` | The input fields. The last one shows while a field has focus. |
| | `find-hint` | Placeholder text in the fields |
| | `find-hover`, `find-active` | Buttons and toggles under the pointer, and selected toggles or pressed buttons |
| | `find-error` | Error text in the panel. Go to line uses the panel's styles, so its error message is this color too. |
| | `find-match`, `find-match-current`, `find-match-current-border` | Every match in the text, the current match, and the outline of the current match |
| Folding | `fold-header` | The background of a line that starts a folded block |
| | `fold-marker`, `fold-marker-text` | The small marker on a folded line, and the text inside it |
| Diagnostics | `diag-error`, `diag-warning`, `diag-info` | The wavy underline for each severity, and the accent edge of the hover box |
| | `diag-tooltip-background`, `diag-tooltip-foreground`, `diag-tooltip-border` | The hover box |
| Completion | `completion-background`, `completion-foreground`, `completion-border` | The popup |
| | `completion-selected-background`, `completion-selected-foreground` | The highlighted row |
| | `completion-kind` | The small letter in front of a word suggestion. Keyword and variable suggestions use `token-keyword` and `token-variable` for their letters. |

## Going past the tokens

If a token doesn't cover what you want, add your own stylesheet to the editor. The syntax classes look like this in `base.css`:

```css
.code-editor .code-area .text.tok-comment-line {
    -fx-fill: #6a9955;
}
```

A scope such as `comment.line` produces both `tok-comment` and `tok-comment-line`, so you can color the specific name differently from its parent.

## Error messages

A bad theme file throws `ThemeException`. The message has three parts: the source, the field and the reason.

```
night-owl.json: version: is required
night-owl.json: colors: is required
night-owl.json: extends: a theme cannot extend itself
night-owl.json: extends: unknown theme 'nord'. Registered themes: [dark-plus, github-light, light-plus, monokai, solarized-dark, solarized-light]
night-owl.json: colors.backgrund: unknown token. Valid tokens: [background, bracket-match, ...]
night-owl.json: colors.token-keyword: expected #rgb, #rrggbb, #rrggbbaa, rgb(r, g, b) or rgba(r, g, b, a)
night-owl.json: colors.caret: a hex color needs 3, 6 or 8 digits but has 4
```

Like language templates, a theme file has to be strict JSON, so comments and trailing commas are errors.