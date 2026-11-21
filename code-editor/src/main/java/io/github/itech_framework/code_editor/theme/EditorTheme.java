package io.github.itech_framework.code_editor.theme;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class EditorTheme {

    private final String id;
    private final String displayName;
    private final boolean dark;
    private final List<String> stylesheets;

    public EditorTheme(String id, String displayName, boolean dark, String stylesheet) {
        this(id, displayName, dark, List.of(Objects.requireNonNull(stylesheet, "stylesheet")));
    }

    public EditorTheme(String id, String displayName, boolean dark, List<String> stylesheets) {
        this.id = normalize(id);
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.dark = dark;
        Objects.requireNonNull(stylesheets, "stylesheets");
        if (stylesheets.isEmpty()) {
            throw new IllegalArgumentException("A theme needs at least one stylesheet");
        }
        this.stylesheets = List.copyOf(stylesheets);
    }

    public static EditorTheme ofResource(String id, String displayName, boolean dark, Class<?> anchor, String path) {
        URL url = Objects.requireNonNull(anchor, "anchor").getResource(path);
        if (url == null) {
            throw new IllegalArgumentException("Theme stylesheet not found: " + path);
        }
        return new EditorTheme(id, displayName, dark, url.toExternalForm());
    }

    static String normalize(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Theme id must not be blank");
        }
        return id.trim().toLowerCase(Locale.ROOT);
    }

    public EditorTheme derive(String newId, String newDisplayName, boolean newDark, String overrideStylesheet) {
        List<String> list = new ArrayList<>(stylesheets);
        list.add(Objects.requireNonNull(overrideStylesheet, "overrideStylesheet"));
        return new EditorTheme(newId, newDisplayName, newDark, list);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isDark() {
        return dark;
    }

    public String getStylesheet() {
        return stylesheets.get(0);
    }

    public List<String> getStylesheets() {
        return stylesheets;
    }
}