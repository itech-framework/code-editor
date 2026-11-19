package io.github.itech_framework.code_editor.language;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import io.github.itech_framework.code_editor.language.template.TemplateLanguages;

public final class LanguageRegistry {

    public static final String PLAIN_TEXT = "plaintext";

    private static final List<String> BUILT_IN = List.of("json.json", "javascript.json");

    private static final LanguageRegistry DEFAULT = new LanguageRegistry();

    private final Map<String, EditorLanguage> languages = new ConcurrentHashMap<>();

    public LanguageRegistry() {
        register(EditorLanguage.builder(PLAIN_TEXT)
                .displayName("Plain Text")
                .extension("txt")
                .tokenizer(new PlainTextTokenizer())
                .build());
        for (String file : BUILT_IN) {
            register(TemplateLanguages.fromResource(file));
        }
    }

    public static LanguageRegistry getDefault() {
        return DEFAULT;
    }

    public void register(EditorLanguage language) {
        Objects.requireNonNull(language, "language");
        languages.put(language.getId(), language);
    }

    public Optional<EditorLanguage> find(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(languages.get(EditorLanguage.normalizeId(id)));
    }

    public EditorLanguage require(String id) {
        return find(id).orElseThrow(
                () -> new IllegalArgumentException("Unknown language '" + id + "'. Registered languages: " + ids()));
    }

    public Optional<EditorLanguage> findByExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return Optional.empty();
        }
        String value = EditorLanguage.normalizeExtension(extension);
        for (EditorLanguage language : all()) {
            if (language.getExtensions().contains(value)) {
                return Optional.of(language);
            }
        }
        return Optional.empty();
    }

    public Set<String> ids() {
        return new TreeSet<>(languages.keySet());
    }

    public List<EditorLanguage> all() {
        List<EditorLanguage> list = new ArrayList<>(languages.values());
        list.sort(Comparator.comparing(EditorLanguage::getId));
        return list;
    }
}