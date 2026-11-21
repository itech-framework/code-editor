package io.github.itech_framework.code_editor.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import io.github.itech_framework.code_editor.language.LineTokens;
import io.github.itech_framework.code_editor.language.Token;
import io.github.itech_framework.code_editor.language.Tokenizer;
import io.github.itech_framework.code_editor.language.TokenizerState;

public final class HighlightRun {

    public record LineInfo(TokenizerState start, TokenizerState end) {
    }

    public record Result(List<LineInfo> infos, List<List<Token>> tokens, boolean incomplete) {
    }

    private HighlightRun() {
    }

    public static Result run(Tokenizer tokenizer, List<String> lines, TokenizerState startState, List<LineInfo> old,
            int lastDirty, boolean moreAfter, int maxLineLength, BooleanSupplier cancelled) {
        int count = lines.size();
        List<LineInfo> infos = new ArrayList<>(Math.min(count, 64));
        List<List<Token>> tokens = new ArrayList<>(Math.min(count, 64));
        TokenizerState state = startState;
        int i = 0;
        while (i < count) {
            if (cancelled.getAsBoolean()) {
                return null;
            }
            LineInfo previous = old.get(i);
            if (i > lastDirty && previous != null && previous.start().equals(state)) {
                return new Result(infos, tokens, false);
            }
            String line = lines.get(i);
            LineTokens result = line.length() > maxLineLength ? new LineTokens(List.of(), state)
                    : tokenizer.tokenize(line, state);
            infos.add(new LineInfo(state, result.endState()));
            tokens.add(result.tokens());
            state = result.endState();
            i++;
        }
        return new Result(infos, tokens, moreAfter);
    }
}