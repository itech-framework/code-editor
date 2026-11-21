package io.github.itech_framework.code_editor.theme;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ThemeTokens {

    private static final String RESOURCE = "/io/github/itech_framework/code_editor/css/dark-plus.css";

    private static final Pattern TOKEN = Pattern.compile("-ce-([a-z0-9-]+)\\s*:");

    private static volatile Set<String> names;

    private ThemeTokens() {
    }

    public static Set<String> names() {
        Set<String> local = names;
        if (local == null) {
            local = load();
            names = local;
        }
        return local;
    }

    private static Set<String> load() {
        try (InputStream in = ThemeTokens.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + RESOURCE);
            }
            String css = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Set<String> found = new TreeSet<>();
            Matcher matcher = TOKEN.matcher(css);
            while (matcher.find()) {
                found.add(matcher.group(1));
            }
            return Collections.unmodifiableSet(found);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}