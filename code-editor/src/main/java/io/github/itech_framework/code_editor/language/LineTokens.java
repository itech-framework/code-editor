package io.github.itech_framework.code_editor.language;

import java.util.List;
import java.util.Objects;

public record LineTokens(List<Token> tokens, TokenizerState endState) {

    public LineTokens {
        Objects.requireNonNull(endState, "endState");
        tokens = List.copyOf(tokens);
    }
}