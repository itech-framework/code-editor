package io.github.itech_framework.code_editor.language;

public interface Tokenizer {

    TokenizerState initialState();

    LineTokens tokenize(String line, TokenizerState state);
}