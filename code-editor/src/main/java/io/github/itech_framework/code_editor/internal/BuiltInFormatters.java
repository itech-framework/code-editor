package io.github.itech_framework.code_editor.internal;

import io.github.itech_framework.code_editor.formatting.CodeFormatter;
import io.github.itech_framework.code_editor.language.EditorLanguage;
import io.github.itech_framework.code_editor.language.FormatterSpec;

public final class BuiltInFormatters {

    private static final CodeFormatter JSON = new JsonFormatter();

    private BuiltInFormatters() {
    }

    public static CodeFormatter forLanguage(EditorLanguage language) {
        if (language == null) {
            return null;
        }
        return language.getFormatterSpec().map(spec -> create(language, spec)).orElse(null);
    }

    private static CodeFormatter create(EditorLanguage language, FormatterSpec spec) {
        return switch (spec.type()) {
            case JSON -> JSON;
            case BRACE -> new BraceFormatter(language.getTokenizer(), spec);
        };
    }
}