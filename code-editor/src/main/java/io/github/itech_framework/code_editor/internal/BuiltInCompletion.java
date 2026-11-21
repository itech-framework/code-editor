package io.github.itech_framework.code_editor.internal;

import java.util.List;

import io.github.itech_framework.code_editor.completion.CompletionProvider;
import io.github.itech_framework.code_editor.language.EditorLanguage;

public final class BuiltInCompletion {

    private BuiltInCompletion() {
    }

    public static CompletionProvider forLanguage(EditorLanguage language) {
        if (language == null) {
            return null;
        }
        List<String> keywords = language.getKeywords();
        return (text, caret) -> CompletionLogic.suggest(text, caret, keywords);
    }
}