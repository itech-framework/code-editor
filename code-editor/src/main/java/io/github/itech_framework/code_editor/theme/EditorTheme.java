package io.github.itech_framework.code_editor.theme;

import java.net.URL;
import java.util.Locale;
import java.util.Objects;

public final class EditorTheme {

    private final String id;
    private final String displayName;
    private final boolean dark;
    private final String stylesheet;

    public EditorTheme(String id, String displayName, boolean dark, String stylesheet) {
        this.id = normalize(id);
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.dark = dark;
        this.stylesheet = Objects.requireNonNull(stylesheet, "stylesheet");
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
        return stylesheet;
    }
}