package io.github.itech_framework.code_editor.theme;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class ThemeRegistry {

    public static final String DARK_PLUS = "dark-plus";

    public static final String LIGHT_PLUS = "light-plus";

    private static final ThemeRegistry DEFAULT = new ThemeRegistry();

    private final Map<String, EditorTheme> themes = new ConcurrentHashMap<>();

    private final List<WeakReference<Consumer<EditorTheme>>> listeners = new CopyOnWriteArrayList<>();

    public ThemeRegistry() {
        register(EditorTheme.ofResource(DARK_PLUS, "Dark+", true, ThemeRegistry.class,
                "/io/github/itech_framework/code_editor/css/dark-plus.css"));
        register(EditorTheme.ofResource(LIGHT_PLUS, "Light+", false, ThemeRegistry.class,
                "/io/github/itech_framework/code_editor/css/light-plus.css"));
    }

    public static ThemeRegistry getDefault() {
        return DEFAULT;
    }

    public void register(EditorTheme theme) {
        Objects.requireNonNull(theme, "theme");
        themes.put(theme.getId(), theme);
        notifyListeners(theme);
    }

    public void addWeakListener(Consumer<EditorTheme> listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(new WeakReference<>(listener));
    }

    public Optional<EditorTheme> find(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(themes.get(EditorTheme.normalize(id)));
    }

    public EditorTheme require(String id) {
        return find(id).orElseThrow(
                () -> new IllegalArgumentException("Unknown theme '" + id + "'. Registered themes: " + ids()));
    }

    public Set<String> ids() {
        return new TreeSet<>(themes.keySet());
    }

    public List<EditorTheme> all() {
        List<EditorTheme> list = new ArrayList<>(themes.values());
        list.sort(Comparator.comparing(EditorTheme::getDisplayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(EditorTheme::getId));
        return list;
    }

    private void notifyListeners(EditorTheme theme) {
        for (WeakReference<Consumer<EditorTheme>> reference : listeners) {
            Consumer<EditorTheme> listener = reference.get();
            if (listener == null) {
                listeners.remove(reference);
            } else {
                listener.accept(theme);
            }
        }
    }
}