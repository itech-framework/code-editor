package io.github.itech_framework.code_editor.internal;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javafx.scene.text.Font;

public final class FontSupport {

    public static final String MONOSPACE = "monospace";

    private static volatile Map<String, String> installed;

    private FontSupport() {
    }

    public static String resolve(String candidates) {
        if (candidates == null) {
            return MONOSPACE;
        }
        for (String raw : candidates.split(",")) {
            String name = raw.trim().replace("'", "").replace("\"", "");
            if (name.isEmpty()) {
                continue;
            }
            if (name.equalsIgnoreCase(MONOSPACE)) {
                return MONOSPACE;
            }
            String match = installed().get(name.toLowerCase(Locale.ROOT));
            if (match != null) {
                return match;
            }
        }
        return MONOSPACE;
    }

    private static Map<String, String> installed() {
        Map<String, String> map = installed;
        if (map == null) {
            Map<String, String> built = new HashMap<>();
            for (String family : Font.getFamilies()) {
                built.put(family.toLowerCase(Locale.ROOT), family);
            }
            installed = built;
            map = built;
        }
        return map;
    }
}