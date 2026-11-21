package io.github.itech_framework.code_editor.theme;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public final class ThemeSheet {

    private ThemeSheet() {
    }

    public static String dataUri(Map<String, ThemeColor> colors) {
        StringBuilder css = new StringBuilder(".code-editor {\n");
        for (Map.Entry<String, ThemeColor> entry : colors.entrySet()) {
            css.append("    -ce-").append(entry.getKey()).append(": ").append(entry.getValue().toCss()).append(";\n");
        }
        css.append("}\n");
        return "data:text/css;base64," + Base64.getEncoder().encodeToString(css.toString().getBytes(StandardCharsets.UTF_8));
    }
}