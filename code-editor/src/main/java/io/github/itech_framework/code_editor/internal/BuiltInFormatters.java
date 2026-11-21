package io.github.itech_framework.code_editor.internal;

import io.github.itech_framework.code_editor.formatting.CodeFormatter;
import io.github.itech_framework.code_editor.language.EditorLanguage;

public final class BuiltInFormatters {

    private static final CodeFormatter JSON = new JsonFormatter();

    private BuiltInFormatters() {
    }

    public static CodeFormatter forLanguage(EditorLanguage language) {
        if (language == null) {
            return null;
        }
        return "json".equals(language.getId()) ? JSON : null;
    }
}