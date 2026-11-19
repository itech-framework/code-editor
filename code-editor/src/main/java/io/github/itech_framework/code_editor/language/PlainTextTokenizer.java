package io.github.itech_framework.code_editor.language;

import java.util.List;

final class PlainTextTokenizer implements Tokenizer {

    private enum State implements TokenizerState {
        INSTANCE
    }

    private static final LineTokens EMPTY = new LineTokens(List.of(), State.INSTANCE);

    @Override
    public TokenizerState initialState() {
        return State.INSTANCE;
    }

    @Override
    public LineTokens tokenize(String line, TokenizerState state) {
        return EMPTY;
    }
}