package io.github.itech_framework.code_editor.theme;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    public static Builder builder(String id, String displayName, boolean dark) {
        return new Builder(id, displayName, dark);
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

    public static final class Builder {

        private static final String PREFIX = "-ce-";

        private final String id;
        private final String displayName;
        private final boolean dark;
        private String parent;
        private final Map<String, ThemeColor> colors = new LinkedHashMap<>();

        private Builder(String id, String displayName, boolean dark) {
            this.id = normalize(id);
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            this.dark = dark;
        }

        public Builder extending(String parentId) {
            this.parent = Objects.requireNonNull(parentId, "parentId");
            return this;
        }

        public Builder color(String token, String value) {
            Objects.requireNonNull(token, "token");
            Objects.requireNonNull(value, "value");
            String name = token.trim().startsWith(PREFIX) ? token.trim().substring(PREFIX.length()) : token.trim();
            if (!ThemeTokens.names().contains(name)) {
                throw new IllegalArgumentException(
                        "Unknown token '" + token + "'. Valid tokens: " + ThemeTokens.names());
            }
            try {
                colors.put(name, ThemeColor.parse(value));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid color for '" + name + "': " + ex.getMessage(), ex);
            }
            return this;
        }

        public EditorTheme build() {
            return build(ThemeRegistry.getDefault());
        }

        public EditorTheme build(ThemeRegistry registry) {
            Objects.requireNonNull(registry, "registry");
            String parentId = parent != null ? parent : dark ? ThemeRegistry.DARK_PLUS : ThemeRegistry.LIGHT_PLUS;
            if (normalize(parentId).equals(id)) {
                throw new IllegalArgumentException("A theme cannot extend itself: " + id);
            }
            EditorTheme base = registry.require(parentId);
            if (colors.isEmpty()) {
                return new EditorTheme(id, displayName, dark, base.getStylesheets());
            }
            return base.derive(id, displayName, dark, ThemeSheet.dataUri(colors));
        }
    }
}