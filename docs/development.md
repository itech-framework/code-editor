# Development

## What you need

- JDK 17 or newer
- Maven 3.9 or newer
- A display, but only to run the demo. The unit tests don't need one.

The build targets Java 17, JavaFX 17.0.1 and RichTextFX 0.11.5. JUnit 5.11.4 runs the tests.

## Build and test

```
mvn clean install
```

This compiles both modules, runs the unit tests and installs the library into your local Maven repository.

```
mvn -pl code-editor test
mvn -pl code-editor test -Dtest=FoldLogicTest
```

The first line runs every unit test, and the second runs one test class. CI runs `mvn -B -ntp clean verify`.

## Modules

| Module | What it is |
|---|---|
| `code-editor` | The library. JavaFX is a `provided` dependency, so each application brings its own. |
| `code-editor-demo` | A plain JavaFX app for trying things by hand. It is not part of the library. |

The library's build uses the Maven enforcer plugin to fail if anything from the `io.github.itech-framework` group shows up among its dependencies, directly or through another dependency. That keeps the core standalone.

The jar's automatic module name is `io.github.itech_framework.code_editor`.

## Unit tests

Tests live in `code-editor/src/test/java`, in the same packages as the classes they cover. There are 23 test classes. They cover the classes that have no JavaFX in them:

- the editing logic: indent, tab, auto-close, line operations, multi-caret edits, selections, go to line, bracket matching, folding and find
- completion: the word and ranking logic, and the built-in provider
- diagnostics: the JSON, lexical and built-in providers
- formatting: the JSON formatter, the brace formatter and the offset mapping used to keep selections in place
- the highlighter's line cache
- key chord recognition
- the JSON reader and the language template loader

The panels, popup, gutter, skin and features need a JavaFX scene, so they are checked by hand in the demo. A test that needs a display wouldn't run on CI.

When you add logic, put the part that doesn't need JavaFX in an `internal` class ending in `Logic`, and give it a test.

## Continuous integration

`.github/workflows/ci.yml` builds and tests on every push to `main` or `master` and on every pull request. It runs on Ubuntu and Windows with Temurin JDK 17. A newer run of the same branch cancels an older one. If a build fails, the Surefire reports are uploaded as an artifact named `test-reports-<os>`.

macOS and Java versions above 17 are not in the matrix yet.

## The demo app

```
cd code-editor-demo
mvn javafx:run
```

Run `mvn clean install` at the root first, so the demo finds the library.

From an IDE, run `Main`. It is a plain class that calls `Application.launch`, so it starts without the Maven plugin. JavaFX has to be on the classpath.

The demo has toggles for the main settings, a language picker with a loader for your own language templates, a theme picker with a loader for theme files, and a Benchmark button.

## The benchmark

The Benchmark button runs `PerfBench`. It generates JavaScript documents of 100 KB, 1 MB and 5 MB, with some Burmese text in the strings, and measures:

- **load:** how long `setText` takes on the JavaFX thread, and how long until highlighting settles. It counts as settled when no new highlight has arrived for 600 ms. It gives up after 60 seconds and marks the result `TIMED OUT`.
- **getText:** the first call after an edit, and a second call straight after.
- **features on:** how long it takes to switch every feature back on.
- **typing:** 200 characters inserted in the middle of the document, 80 ms apart, once with only the highlighter running and once with all features on.

Two numbers describe how smooth the window stayed. Worst frame is the longest gap between two frames. Stalled is the total time frames ran over 25 ms, beyond the normal 16.7 ms.

The output goes to the console, with `[bench]` at the start of the progress lines, followed by a summary table. The table shows pairs of numbers, highlighter only and then all features, and its columns are size, load, getText first and second, insert average, insert worst, worst frame and stalled. The status bar shows the progress and the last result.

Things to know before you run it:

- Leave the window alone while it runs, and don't type.
- It replaces the editor's text and switches features off and on. When it finishes, or fails, it puts back the text, the language, the feature states and the auto-open setting.
- The 5 MB document is above the limits for folding (2,000,000 characters) and for lexical diagnostics and the brace formatter (1,000,000), so that run shows the editor with those off. See [architecture.md](architecture.md).

## Adding a built-in feature

1. Put anything that doesn't need JavaFX in an `internal` class ending in `Logic`, with a test.
2. Implement `EditorFeature` in the `feature` package, with a public `ID` constant like the others.
3. Register it in the `CodeEditor` constructor. Order matters for keys. Handlers on the same node run in the order they were registered, even after one of them consumes the event. A feature that has to see Enter or Tab before auto-indent and tab-indent does, as completion does, is registered before them. The later ones check `isConsumed()` first.
4. Start every key handler with `isTextAreaEvent(event)`. Skip your edit when `hasExtraCarets()` is true unless you handle every caret.
5. If the feature adds style classes to the text, apply them again from a highlight listener, because the highlighter rewrites the styles of any line it re-colors.
6. Add the keys to [shortcuts.md](shortcuts.md) and the id to the lists in [extending.md](extending.md) and the README.
7. Check it in the demo, since the tests can't reach it.

## Publishing

The library is not published yet, and the build isn't ready for Maven Central. What is missing:

- Project metadata in the parent POM: `url`, `scm` and `developers`. The name, description and license are already there.
- A sources jar and a javadoc jar. Central requires both. The code has almost no Javadoc, so the javadoc jar would be nearly empty, and the API reference lives in [extending.md](extending.md).
- Signed artifacts.
- A release version in place of `0.1.0-SNAPSHOT`, and the real coordinates in the README.