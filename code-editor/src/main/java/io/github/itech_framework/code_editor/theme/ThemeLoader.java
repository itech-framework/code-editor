package io.github.itech_framework.code_editor.theme;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import io.github.itech_framework.code_editor.internal.json.JsonException;
import io.github.itech_framework.code_editor.internal.json.JsonReader;

public final class ThemeLoader {

    public static final int SUPPORTED_VERSION = 1;

    private static final Set<String> KEYS = Set.of("version", "id", "name", "dark", "extends", "colors");

    private final String source;

    private final ThemeRegistry registry;

    private ThemeLoader(String source, ThemeRegistry registry) {
        this.source = source;
        this.registry = registry;
    }

    public static EditorTheme load(String source, String json) {
        return load(source, json, ThemeRegistry.getDefault());
    }

    public static EditorTheme load(String source, String json, ThemeRegistry registry) {
        Object parsed;
        try {
            parsed = JsonReader.parse(json);
        } catch (JsonException ex) {
            throw new ThemeException(source, "json", ex.getMessage(), ex);
        }
        return new ThemeLoader(source, registry).build(parsed);
    }

    public static EditorTheme load(Path path) throws IOException {
        return load(path, ThemeRegistry.getDefault());
    }

    public static EditorTheme load(Path path, ThemeRegistry registry) throws IOException {
        return load(path.toString(), Files.readString(path, StandardCharsets.UTF_8), registry);
    }

    private EditorTheme build(Object root) {
        if (!(root instanceof Map<?, ?>)) {
            throw fail("(root)", "must be an object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) root;
        for (String key : map.keySet()) {
            if (!KEYS.contains(key)) {
                throw fail(key, "unknown field");
            }
        }

        Object version = map.get("version");
        if (version == null) {
            throw fail("version", "is required");
        }
        if (!(version instanceof Long number)) {
            throw fail("version", "must be an integer");
        }
        if (number != SUPPORTED_VERSION) {
            throw fail("version", "unsupported value " + number + ", expected " + SUPPORTED_VERSION);
        }

        String id = string(map, "id", true);
        String name = string(map, "name", false);
        String displayName = name == null ? id : name;

        boolean dark = false;
        Object darkValue = map.get("dark");
        if (darkValue != null) {
            if (!(darkValue instanceof Boolean flag)) {
                throw fail("dark", "must be true or false");
            }
            dark = flag;
        }

        String parentId = string(map, "extends", false);
        if (parentId == null) {
            parentId = dark ? ThemeRegistry.DARK_PLUS : ThemeRegistry.LIGHT_PLUS;
        } else if (parentId.trim().equalsIgnoreCase(id.trim())) {
            throw fail("extends", "a theme cannot extend itself");
        }
        String requested = parentId;
        EditorTheme parent = registry.find(parentId).orElseThrow(() -> fail("extends",
                "unknown theme '" + requested + "'. Registered themes: " + registry.ids()));

        Object colorsValue = map.get("colors");
        if (colorsValue == null) {
            throw fail("colors", "is required");
        }
        if (!(colorsValue instanceof Map<?, ?> rawColors)) {
            throw fail("colors", "must be an object");
        }
        Map<String, ThemeColor> colors = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawColors.entrySet()) {
            String token = (String) entry.getKey();
            String field = "colors." + token;
            if (!ThemeTokens.names().contains(token)) {
                throw fail(field, "unknown token. Valid tokens: " + ThemeTokens.names());
            }
            if (!(entry.getValue() instanceof String text)) {
                throw fail(field, "must be a string");
            }
            try {
                colors.put(token, ThemeColor.parse(text));
            } catch (IllegalArgumentException ex) {
                throw fail(field, ex.getMessage());
            }
        }

        if (colors.isEmpty()) {
            return new EditorTheme(id, displayName, dark, parent.getStylesheets());
        }
        return parent.derive(id, displayName, dark, ThemeSheet.dataUri(colors));
    }

    private String string(Map<String, Object> map, String key, boolean required) {
        Object value = map.get(key);
        if (value == null) {
            if (required) {
                throw fail(key, "is required");
            }
            return null;
        }
        if (!(value instanceof String text)) {
            throw fail(key, "must be a string");
        }
        if (text.isBlank()) {
            throw fail(key, "must not be blank");
        }
        return text;
    }

    private ThemeException fail(String field, String reason) {
        return new ThemeException(source, field, reason);
    }
}