package io.github.itech_framework.code_editor.language.template;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.itech_framework.code_editor.language.BracketPair;
import io.github.itech_framework.code_editor.language.FormatterSpec;

public record LanguageTemplate(
        int version,
        String id,
        String displayName,
        List<String> extensions,
        String startState,
        List<BracketPair> autoClosingPairs,
        String lineComment,
        FormatterSpec formatter,
        Map<String, List<TemplateRule>> states) {

    public LanguageTemplate {
        extensions = List.copyOf(extensions);
        autoClosingPairs = List.copyOf(autoClosingPairs);
        Map<String, List<TemplateRule>> copy = new LinkedHashMap<>();
        states.forEach((name, rules) -> copy.put(name, List.copyOf(rules)));
        states = Collections.unmodifiableMap(copy);
    }
}