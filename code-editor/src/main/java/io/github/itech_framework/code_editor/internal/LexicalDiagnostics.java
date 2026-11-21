package io.github.itech_framework.code_editor.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import io.github.itech_framework.code_editor.diagnostics.Diagnostic;
import io.github.itech_framework.code_editor.diagnostics.DiagnosticSeverity;
import io.github.itech_framework.code_editor.diagnostics.DiagnosticsProvider;
import io.github.itech_framework.code_editor.language.LineTokens;
import io.github.itech_framework.code_editor.language.Token;
import io.github.itech_framework.code_editor.language.Tokenizer;
import io.github.itech_framework.code_editor.language.TokenizerState;
import io.github.itech_framework.code_editor.language.template.TemplateTokenizer;

public final class LexicalDiagnostics implements DiagnosticsProvider {

    public static final int MAX_TEXT_LENGTH = 1_000_000;

    public static final int MAX_FOUND = 1000;

    private static final String OPENING = "([{";

    private static final String CLOSING = ")]}";

    private record Open(char symbol, int offset) {
    }

    private record Block(int start, int end, String scope) {
    }

    private final Tokenizer tokenizer;

    public LexicalDiagnostics(Tokenizer tokenizer) {
        this.tokenizer = Objects.requireNonNull(tokenizer, "tokenizer");
    }

    @Override
    public List<Diagnostic> diagnose(String text) {
        if (text.isEmpty() || text.length() > MAX_TEXT_LENGTH) {
            return List.of();
        }
        List<Diagnostic> found = new ArrayList<>();
        Deque<Open> stack = new ArrayDeque<>();
        TokenizerState initial = tokenizer.initialState();
        TokenizerState state = initial;
        Block block = null;
        int lineStart = 0;
        while (found.size() < MAX_FOUND) {
            int newline = text.indexOf('\n', lineStart);
            int lineEnd = newline < 0 ? text.length() : newline;
            String line = text.substring(lineStart, lineEnd);
            boolean atRoot = state.equals(initial);
            LineTokens result = tokenizer.tokenize(line, state);
            List<Token> tokens = result.tokens();
            if (line.length() <= TemplateTokenizer.MAX_LINE_LENGTH) {
                if (atRoot) {
                    checkStrings(line, lineStart, tokens, found);
                }
                if (atRoot || !tokens.isEmpty()) {
                    checkBrackets(line, lineStart, tokens, stack, found);
                }
            }
            boolean endsAtRoot = result.endState().equals(initial);
            if (endsAtRoot) {
                block = null;
            } else if (atRoot) {
                block = null;
                if (!tokens.isEmpty()) {
                    Token last = tokens.get(tokens.size() - 1);
                    block = new Block(lineStart + last.start(), lineStart + last.end(), last.scope());
                }
            }
            state = result.endState();
            if (newline < 0) {
                break;
            }
            lineStart = newline + 1;
        }
        if (block != null) {
            if (block.scope().startsWith("comment")) {
                found.add(error(block.start(), block.end(), "Unterminated comment"));
            } else if (block.scope().startsWith("string")) {
                found.add(error(block.start(), block.end(), "Unterminated string"));
            }
        }
        for (Open open : stack) {
            found.add(error(open.offset(), open.offset() + 1, "Unclosed '" + open.symbol() + "'"));
        }
        return List.copyOf(found);
    }

    private static void checkStrings(String line, int lineStart, List<Token> tokens, List<Diagnostic> found) {
        for (Token token : tokens) {
            int start = token.start();
            int end = Math.min(token.end(), line.length());
            if (end != line.length() || end - start < 1 || !token.scope().startsWith("string")) {
                continue;
            }
            char quote = line.charAt(start);
            if (quote != '"' && quote != '\'') {
                continue;
            }
            int closing = -1;
            for (int i = start + 1; i < end; i++) {
                char c = line.charAt(i);
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    closing = i;
                    break;
                }
            }
            if (closing < 0) {
                found.add(error(lineStart + start, lineStart + end, "Unterminated string"));
            }
        }
    }

    private static void checkBrackets(String line, int lineStart, List<Token> tokens, Deque<Open> stack,
            List<Diagnostic> found) {
        int next = 0;
        for (int i = 0; i < line.length(); i++) {
            while (next < tokens.size() && tokens.get(next).end() <= i) {
                next++;
            }
            if (next < tokens.size() && tokens.get(next).start() <= i && isIgnored(tokens.get(next).scope())) {
                i = Math.min(tokens.get(next).end(), line.length()) - 1;
                continue;
            }
            char c = line.charAt(i);
            if (OPENING.indexOf(c) >= 0) {
                stack.push(new Open(c, lineStart + i));
            } else if (CLOSING.indexOf(c) >= 0) {
                close(c, lineStart + i, stack, found);
            }
        }
    }

    private static void close(char closing, int offset, Deque<Open> stack, List<Diagnostic> found) {
        char expected = OPENING.charAt(CLOSING.indexOf(closing));
        int depth = 0;
        for (Open open : stack) {
            if (open.symbol() == expected) {
                break;
            }
            depth++;
        }
        if (depth == stack.size()) {
            found.add(error(offset, offset + 1, "Unmatched '" + closing + "'"));
            return;
        }
        for (int i = 0; i < depth; i++) {
            Open skipped = stack.pop();
            found.add(error(skipped.offset(), skipped.offset() + 1, "Unclosed '" + skipped.symbol() + "'"));
        }
        stack.pop();
    }

    private static boolean isIgnored(String scope) {
        return scope.startsWith("string") || scope.startsWith("comment") || scope.startsWith("regexp");
    }

    private static Diagnostic error(int start, int end, String message) {
        return new Diagnostic(start, end, DiagnosticSeverity.ERROR, message);
    }
}