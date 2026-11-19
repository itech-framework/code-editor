package io.github.itech_framework.code_editor.theme;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public final class ThemeManager {

    public static final String DARK_PLUS = "dark-plus";

    private static final Map<String, EditorTheme> THEMES = new ConcurrentHashMap<>();

    static {
        register(EditorTheme.ofResource(DARK_PLUS, "Dark+", true, ThemeManager.class,
                "/io/github/itech_framework/code_editor/css/dark-plus.css"));
    }

    private ThemeManager() {
    }

    public static void register(EditorTheme theme) {
        Objects.requireNonNull(theme, "theme");
        THEMES.put(theme.getId(), theme);
    }

    public static Optional<EditorTheme> find(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(THEMES.get(EditorTheme.normalize(id)));
    }

    public static EditorTheme require(String id) {
        return find(id).orElseThrow(
                () -> new IllegalArgumentException("Unknown theme '" + id + "'. Registered themes: " + ids()));
    }

    public static Set<String> ids() {
        return new TreeSet<>(THEMES.keySet());
    }
}