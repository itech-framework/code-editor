# Code Editor for JavaFX

A reusable, Monaco-inspired code editor component for JavaFX, built on [RichTextFX](https://github.com/FXMisc/RichTextFX).

- Pure JavaFX. No Swing and no embedded browser.
- Standalone. It depends only on JavaFX and RichTextFX.
- Syntax highlighting from small JSON language templates.
- Editing features you expect from a code editor: auto-indent, auto-close, bracket matching, find and replace, code folding, multiple carets, autocomplete, formatting and diagnostics.
- Works with Burmese and other scripts that use combining marks.
- Every feature can be switched off, and completion, diagnostics and formatting can be replaced with your own code.

## Status

Version `0.1.0-SNAPSHOT`. It is not published to Maven Central yet, so you build it from source (see below). The API can still change before the first release.

## Requirements

| Item | Version |
|---|---|
| Java | 17 or newer |
| JavaFX | 17 (the library is built and tested against 17.0.1) |
| RichTextFX | 0.11.5 (brought in by the library) |

JavaFX is a `provided` dependency, so your application must supply `javafx-controls` itself.

RichTextFX 0.11.7 is needed for JavaFX 25 and newer. That combination has not been tested.

## Build from source

```
mvn clean install
```

This builds the library, runs the unit tests and installs it into your local Maven repository.

## Add it to your project

```xml
<dependency>
    <groupId>io.github.itech-framework</groupId>
    <artifactId>code-editor</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>17.0.1</version>
</dependency>
```

The jar has the automatic module name `io.github.itech_framework.code_editor`. Modular applications can use `requires io.github.itech_framework.code_editor;`.

## Quick start

```java
import io.github.itech_framework.code_editor.CodeEditor;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        CodeEditor editor = new CodeEditor();
        editor.setLanguage("json");
        editor.setTheme("dark-plus");
        editor.setText("{\n  \"name\": \"demo\",\n  \"tags\": [1, 2, 3]\n}");

        stage.setScene(new Scene(editor, 900, 600));
        stage.setTitle("Code Editor");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

Read the text back with `editor.getText()`.

## Languages

Built in: `plaintext`, `json` and `javascript`.

A Java template ships with the library but is not registered by default. Register it before you select it:

```java
LanguageRegistry.getDefault().register(TemplateLanguages.fromResource("java.json"));
editor.setLanguage("java");
```

Languages are described by JSON templates, so you can add your own without writing a tokenizer. You can also implement the `Tokenizer` interface for full control.

## Themes

Built in: `dark-plus` (default), `light-plus`, `monokai`, `github-light`, `solarized-dark` and `solarized-light`. Switch with `editor.setTheme("monokai")`. Custom themes can be loaded from a JSON file.

## Features

Each feature has an id. Turn one on or off with:

```java
editor.features().setEnabled("fold", false);
```

| Id | What it does |
|---|---|
| `auto-indent` | Keeps the indent of the line above and adds a level after an opening bracket |
| `auto-close` | Closes brackets and quotes as you type |
| `tab-indent` | Tab and Shift+Tab indent and unindent, using spaces or tabs |
| `bracket-match` | Highlights the matching bracket of `()`, `[]` and `{}` |
| `find` | Find and replace panel with case, whole word and regex options |
| `go-to-line` | Jump to `line` or `line:column` |
| `line-edit` | Move, duplicate and delete lines, and toggle line comments |
| `fold` | Fold and unfold brace and bracket blocks from the gutter |
| `multi-caret` | Multiple carets and selections, select next occurrence and select all occurrences |
| `completion` | Autocomplete popup with keywords and words from the document |
| `format` | Format the whole document |
| `diagnostics` | Wavy underlines for errors |
| `diagnostic-hover` | Shows the message when the mouse rests on an underline |

All key bindings are listed in [docs/shortcuts.md](docs/shortcuts.md).

## Customize

You can plug in your own code through small interfaces:

```java
editor.setCompletionProvider(provider);
editor.setDiagnosticsProvider(provider);
editor.setFormatter(formatter);
```

Editor settings are JavaFX properties: `editable`, `showLineNumbers`, `highlightCurrentLine`, `fontFamily`, `fontSize`, `theme`, `language`, `tabSize`, `insertSpaces`, `completionAutoOpen` and `completionMinChars`. Diagnostics are also published as `diagnosticsProperty()` and `errorCountProperty()`.

## Documentation

- [Keyboard shortcuts](docs/shortcuts.md)
- [Language templates](docs/languages.md)
- [Themes](docs/themes.md)
- [Extending the editor](docs/extending.md)
- [Architecture and limits](docs/architecture.md)
- [Development](docs/development.md)

## Run the demo

After `mvn clean install`:

```
cd code-editor-demo
mvn javafx:run
```

The demo has toggles for the main settings, a language and theme picker, and a Benchmark button for large-document timing.

## Known limitations

- Keyboard shortcuts have been checked on the US layout only. Shortcuts that need a symbol key, such as Ctrl+/ and Ctrl+Shift+[, may differ on other layouts.
- Arrow keys do not move several carets together. They clear the extra carets.
- There is no column (box) selection.
- Very large documents switch some features off. See [docs/architecture.md](docs/architecture.md).
- CI runs on Ubuntu and Windows with JDK 17. macOS, Java 21 and newer JavaFX versions have not been tested yet.

## License

MIT. See [LICENSE](LICENSE).