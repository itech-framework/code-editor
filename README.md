# Code Editor for JavaFX

[![CI](https://github.com/itech-framework/code-editor/actions/workflows/ci.yml/badge.svg)](https://github.com/itech-framework/code-editor/actions/workflows/ci.yml)

A reusable, Monaco-inspired code editor component for JavaFX, built on [RichTextFX](https://github.com/FXMisc/RichTextFX).

- Pure JavaFX. No Swing and no embedded browser.
- Standalone. It depends only on JavaFX and RichTextFX.
- Syntax highlighting from small JSON language templates.
- Auto-indent, auto-close, bracket matching, find and replace, code folding, multiple carets, autocomplete, formatting and diagnostics.
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

To build from source you also need Maven.

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

Languages are described by JSON templates, so you can add your own without writing a tokenizer. You can also implement the `Tokenizer` interface for full control. The template format is in [docs/languages.md](docs/languages.md).

## Themes

Built in: `dark-plus` (default), `light-plus`, `monokai`, `github-light`, `solarized-dark` and `solarized-light`. Switch with `editor.setTheme("monokai")`. Custom themes can be loaded from a JSON file or built in code. See [docs/themes.md](docs/themes.md).

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
| `diagnostics` | Wavy underlines for errors, warnings and info |
| `diagnostic-hover` | Shows the message when the mouse rests on an underline |

All key bindings are listed in [docs/shortcuts.md](docs/shortcuts.md).

## Customize

You can plug in your own code through small interfaces:

```java
editor.setCompletionProvider(provider);
editor.setDiagnosticsProvider(provider);
editor.setFormatter(formatter);
```

The providers run on a background thread and receive a copy of the text. [docs/extending.md](docs/extending.md) has the interfaces, worked examples, and how to write your own feature.

Editor settings are JavaFX properties: `editable`, `showLineNumbers`, `highlightCurrentLine`, `fontFamily`, `fontSize`, `theme`, `language`, `tabSize`, `insertSpaces`, `completionAutoOpen` and `completionMinChars`. Diagnostics are also published as `diagnosticsProperty()` and `errorCountProperty()`.

## Documentation

- [Keyboard shortcuts](docs/shortcuts.md): every key binding, grouped by feature
- [Language templates](docs/languages.md): the JSON format for describing a language
- [Themes](docs/themes.md): the built-in themes, the color tokens and custom theme files
- [Extending the editor](docs/extending.md): the public API, the provider interfaces and custom features
- [Architecture and limits](docs/architecture.md): how highlighting and threading work, size limits and known gaps
- [Development](docs/development.md): building, tests, CI, the demo and the benchmark

## Run the demo

After `mvn clean install`:

```
cd code-editor-demo
mvn javafx:run
```

From an IDE, run the `Main` class in the demo module. The demo has toggles for the main settings, a language and theme picker, and a Benchmark button for large-document timing.

## Known limitations

- Keyboard shortcuts have been checked on the US layout only. Shortcuts that need a symbol key, such as Ctrl+/ and Ctrl+Shift+[, may differ on other layouts.
- Arrow keys do not move several carets together. They clear the extra carets.
- There is no column (box) selection.
- Folding works on `{}` and `[]` blocks only, and it switches off above 2,000,000 characters. Lexical diagnostics and the brace formatter switch off above 1,000,000. The other size limits are in [docs/architecture.md](docs/architecture.md).
- Built-in autocomplete offers keywords and words from the document. For anything smarter, such as the methods of a variable, plug in your own provider.
- CI runs on Ubuntu and Windows with JDK 17. macOS, Java 21 and newer JavaFX versions have not been tested yet.

## License

MIT. See [LICENSE](LICENSE).