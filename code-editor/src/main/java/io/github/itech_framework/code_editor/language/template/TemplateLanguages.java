package io.github.itech_framework.code_editor.language.template;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import io.github.itech_framework.code_editor.language.EditorLanguage;

public final class TemplateLanguages {

    private static final String BASE = "/io/github/itech_framework/code_editor/languages/";

    private TemplateLanguages() {
    }

    public static EditorLanguage fromTemplate(LanguageTemplate template) {
        Objects.requireNonNull(template, "template");
        return EditorLanguage.builder(template.id())
                .displayName(template.displayName())
                .extensions(template.extensions())
                .autoClosingPairs(template.autoClosingPairs())
                .lineComment(template.lineComment())
                .formatter(template.formatter())
                .tokenizer(new TemplateTokenizer(template))
                .build();
    }

    public static EditorLanguage fromResource(String name) {
        String path = BASE + name;
        try (InputStream in = TemplateLanguages.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing built-in language template " + path);
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return fromTemplate(TemplateLoader.load(path, json));
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read built-in language template " + path, ex);
        }
    }
}