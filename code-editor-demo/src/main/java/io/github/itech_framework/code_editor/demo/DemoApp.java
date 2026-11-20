package io.github.itech_framework.code_editor.demo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.github.itech_framework.code_editor.CodeEditor;
import io.github.itech_framework.code_editor.feature.AutoCloseFeature;
import io.github.itech_framework.code_editor.feature.AutoIndentFeature;
import io.github.itech_framework.code_editor.language.BracketPair;
import io.github.itech_framework.code_editor.language.EditorLanguage;
import io.github.itech_framework.code_editor.language.LanguageRegistry;
import io.github.itech_framework.code_editor.language.template.LanguageTemplate;
import io.github.itech_framework.code_editor.language.template.TemplateException;
import io.github.itech_framework.code_editor.language.template.TemplateLanguages;
import io.github.itech_framework.code_editor.language.template.TemplateLoader;
import io.github.itech_framework.code_editor.theme.ThemeManager;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
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

    private static final String JAVA_SAMPLE = """
            package com.flexitech.demo;

            import java.util.List;
            import static java.lang.Math.max;

            /**
             * Simple order service.
             */
            @Service
            public class OrderService<T extends Order> {

                private static final int MAX_ITEMS = 100;
                private final List<T> orders = new ArrayList<>();

                @Override
                public String toString() {
                    return "Orders: " + orders.size() + " မင်္ဂလာပါ";
                }

                public void add(T order) {
                    this.orders.add(order);
                }

                public int total(List<Integer> prices) {
                    int sum = 0; // running total
                    for (int price : prices) {
                        if (price > 0x1F && sum < MAX_ITEMS) {
                            sum += price;
                        }
                    }
                    return max(sum, 0);
                }
            }

            public record Point(int x, int y) {}
            """;

    @Override
    public void start(Stage stage) {
        LanguageRegistry.getDefault().register(EditorLanguage.builder("demo-custom")
                .displayName("Demo Custom")
                .extension("demo")
                .tokenizer(new DemoTokenizer())
                .autoClosingPairs(List.of(new BracketPair('(', ')'), new BracketPair('"', '"')))
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

        CheckBox autoIndent = new CheckBox("Auto indent");
        autoIndent.setSelected(editor.features().isEnabled(AutoIndentFeature.ID));
        autoIndent.selectedProperty().addListener(
                (obs, old, value) -> editor.features().setEnabled(AutoIndentFeature.ID, value));

        CheckBox autoClose = new CheckBox("Auto close");
        autoClose.setSelected(editor.features().isEnabled(AutoCloseFeature.ID));
        autoClose.selectedProperty().addListener(
                (obs, old, value) -> editor.features().setEnabled(AutoCloseFeature.ID, value));

        Spinner<Integer> tabSize = new Spinner<>(1, 16, editor.getTabSize());
        tabSize.valueProperty().addListener((obs, old, value) -> editor.setTabSize(value));

        CheckBox spaces = new CheckBox("Spaces");
        spaces.selectedProperty().bindBidirectional(editor.insertSpacesProperty());

        Spinner<Integer> size = new Spinner<>(8, 40, 14);
        size.valueProperty().addListener((obs, old, value) -> editor.setFontSize(value));
        
        ComboBox<String> theme = new ComboBox<>(FXCollections.observableArrayList(ThemeManager.ids()));
        theme.setValue(editor.getTheme());
        theme.valueProperty().addListener((obs, old, value) -> {
            if (value != null) {
                editor.setTheme(value);
            }
        });

        Label status = new Label();
        Runnable refresh = () -> status.setText(editor.getLength() + " chars | language: " + editor.getLanguage());
        refresh.run();
        editor.textProperty().addListener((obs, old, value) -> refresh.run());

        ObservableList<String> languageIds = FXCollections.observableArrayList(LanguageRegistry.getDefault().ids());
        ComboBox<String> language = new ComboBox<>(languageIds);
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

        Button sample = new Button("Insert Java sample");
        sample.setOnAction(event -> editor.setText(JAVA_SAMPLE));

        Button load = new Button("Load language template...");
        load.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select a language template");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Language templates (*.json)", "*.json"));
            Path folder = Path.of("languages");
            if (Files.isDirectory(folder)) {
                chooser.setInitialDirectory(folder.toFile());
            }
            File file = chooser.showOpenDialog(stage);
            if (file == null) {
                return;
            }
            try {
                LanguageTemplate template = TemplateLoader.load(file.toPath());
                EditorLanguage loaded = TemplateLanguages.fromTemplate(template);
                LanguageRegistry.getDefault().register(loaded);
                if (!languageIds.contains(loaded.getId())) {
                    languageIds.add(loaded.getId());
                }
                editor.setLanguage(LanguageRegistry.PLAIN_TEXT);
                editor.setLanguage(loaded.getId());
                language.setValue(loaded.getId());
                refresh.run();
            } catch (TemplateException ex) {
                status.setText(ex.getMessage());
            } catch (IOException ex) {
                status.setText("Cannot read " + file + ": " + ex.getMessage());
            }
        });

        ToolBar bar = new ToolBar(editable, lineNumbers, currentLine, autoIndent, autoClose, new Label("Tab size"),
                tabSize, spaces, new Label("Font size"), size, new Label("Theme"), theme, new Label("Language"),
                language, unknown, sample, load);
        
        BorderPane root = new BorderPane(editor);
        root.setTop(bar);
        root.setBottom(status);
        BorderPane.setMargin(status, new Insets(4, 8, 4, 8));

        stage.setTitle("Code Editor Demo");
        stage.setScene(new Scene(root, 1300, 650));
        stage.setMaximized(true);
        stage.show();
    }
}