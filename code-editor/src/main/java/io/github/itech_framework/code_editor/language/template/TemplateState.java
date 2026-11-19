package io.github.itech_framework.code_editor.language.template;

import java.util.List;

import io.github.itech_framework.code_editor.language.TokenizerState;

public record TemplateState(List<String> stack, TemplateRule.Block block) implements TokenizerState {

    public TemplateState {
        stack = List.copyOf(stack);
    }
}