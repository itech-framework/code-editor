package io.github.itech_framework.code_editor.completion;

import java.util.List;

@FunctionalInterface
public interface CompletionProvider {

    List<CompletionItem> complete(String text, int caret);

    default String triggerCharacters() {
        return "";
    }
}