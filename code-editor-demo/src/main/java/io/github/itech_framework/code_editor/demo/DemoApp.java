package io.github.itech_framework.code_editor.demo;

import io.github.itech_framework.code_editor.CodeEditor;
import io.github.itech_framework.code_editor.language.EditorLanguage;
import io.github.itech_framework.code_editor.language.LanguageRegistry;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class DemoApp extends Application {

    private static final String SAMPLE = """
            // Courier request
            {
              "name": "Courier",
              "greeting": "မင်္ဂလာပါ 😀",
              "items": [1, 2.5, -3],
              "active": true,
              "note": null
            }
            /* a block comment
               spanning two lines */
            if (true) { return 42; }
            """;

    @Override
    public void start(Stage stage) {
        LanguageRegistry.getDefault().register(EditorLanguage.builder("demo-custom")
                .displayName("Demo Custom")
                .extension("demo")
                .tokenizer(new DemoTokenizer())
                .build());

        CodeEditor editor = new CodeEditor();
        editor.setLanguage("demo-custom");
        editor.setText(SAMPLE);

        CheckBox editable = new CheckBox("Editable");
        editable.selectedProperty().bindBidirectional(editor.editableProperty());

        CheckBox lineNumbers = new CheckBox("Line numbers");
        lineNumbers.selectedProperty().bindBidirectional(editor.showLineNumbersProperty());

        CheckBox currentLine = new CheckBox("Current line");
        currentLine.selectedProperty().bindBidirectional(editor.highlightCurrentLineProperty());

        Spinner<Integer> size = new Spinner<>(8, 40, 14);
        size.valueProperty().addListener((obs, old, value) -> editor.setFontSize(value));

        Label status = new Label();
        Runnable refresh = () -> status.setText(editor.getLength() + " chars | language: " + editor.getLanguage());
        refresh.run();
        editor.textProperty().addListener((obs, old, value) -> refresh.run());

        ComboBox<String> language = new ComboBox<>(
                FXCollections.observableArrayList(LanguageRegistry.getDefault().ids()));
        language.setValue(editor.getLanguage());
        language.valueProperty().addListener((obs, old, value) -> {
            if (value != null) {
                editor.setLanguage(value);
                refresh.run();
            }
        });

        Button unknown = new Button("Set unknown language");
        unknown.setOnAction(event -> {
            try {
                editor.setLanguage("does-not-exist");
            } catch (IllegalArgumentException ex) {
                status.setText(ex.getMessage());
            }
        });

        ToolBar bar = new ToolBar(editable, lineNumbers, currentLine, new Label("Font size"), size,
                new Label("Language"), language, unknown);
        BorderPane root = new BorderPane(editor);
        root.setTop(bar);
        root.setBottom(status);
        BorderPane.setMargin(status, new Insets(4, 8, 4, 8));

        stage.setTitle("Code Editor Demo");
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }
}