package io.github.itech_framework.code_editor;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class EditorFeatures {

    private static final class Entry {

        private final EditorFeature feature;
        private boolean enabled;

        private Entry(EditorFeature feature) {
            this.feature = feature;
        }
    }

    private final CodeEditor editor;
    private final Map<String, Entry> entries = new LinkedHashMap<>();

    EditorFeatures(CodeEditor editor) {
        this.editor = editor;
    }

    public void register(EditorFeature feature) {
        Objects.requireNonNull(feature, "feature");
        String id = normalize(feature.getId());
        Entry previous = entries.get(id);
        boolean enable = previous == null || previous.enabled;
        if (previous != null && previous.enabled) {
            previous.enabled = false;
            previous.feature.uninstall(editor);
        }
        Entry entry = new Entry(feature);
        entries.put(id, entry);
        if (enable) {
            feature.install(editor);
            entry.enabled = true;
        }
    }

    public boolean isEnabled(String id) {
        return require(id).enabled;
    }

    public void setEnabled(String id, boolean enabled) {
        Entry entry = require(id);
        if (entry.enabled == enabled) {
            return;
        }
        if (enabled) {
            entry.feature.install(editor);
            entry.enabled = true;
        } else {
            entry.enabled = false;
            entry.feature.uninstall(editor);
        }
    }

    public Optional<EditorFeature> find(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        Entry entry = entries.get(normalize(id));
        return entry == null ? Optional.empty() : Optional.of(entry.feature);
    }

    public Set<String> ids() {
        return new LinkedHashSet<>(entries.keySet());
    }

    private Entry require(String id) {
        Entry entry = id == null || id.isBlank() ? null : entries.get(normalize(id));
        if (entry == null) {
            throw new IllegalArgumentException("Unknown feature '" + id + "'. Registered features: " + ids());
        }
        return entry;
    }

    private static String normalize(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Feature id must not be blank");
        }
        return id.trim().toLowerCase(Locale.ROOT);
    }
}