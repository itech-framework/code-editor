package io.github.itech_framework.code_editor.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.itech_framework.code_editor.language.LineTokens;
import io.github.itech_framework.code_editor.language.Token;
import io.github.itech_framework.code_editor.language.Tokenizer;
import io.github.itech_framework.code_editor.language.TokenizerState;

final class DemoTokenizer implements Tokenizer {

    private enum State implements TokenizerState {
        NORMAL, BLOCK_COMMENT
    }

    private static final Pattern PATTERN = Pattern.compile("(?<comment>//.*)|(?<open>/\\*)"
            + "|(?<string>\"(?:[^\"\\\\]|\\\\.)*\"?)|(?<number>-?\\d+(?:\\.\\d+)?)"
            + "|(?<word>[A-Za-z_]\\w*)|(?<punct>[{}\\[\\](),:;])");

    @Override
    public TokenizerState initialState() {
        return State.NORMAL;
    }

    @Override
    public LineTokens tokenize(String line, TokenizerState state) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0;
        if (state == State.BLOCK_COMMENT) {
            int close = line.indexOf("*/");
            if (close < 0) {
                if (!line.isEmpty()) {
                    tokens.add(new Token(0, line.length(), "comment.block"));
                }
                return new LineTokens(tokens, State.BLOCK_COMMENT);
            }
            pos = close + 2;
            tokens.add(new Token(0, pos, "comment.block"));
        }
        Matcher matcher = PATTERN.matcher(line);
        while (pos < line.length() && matcher.find(pos)) {
            int start = matcher.start();
            int end = matcher.end();
            if (matcher.group("comment") != null) {
                tokens.add(new Token(start, end, "comment.line"));
                return new LineTokens(tokens, State.NORMAL);
            }
            if (matcher.group("open") != null) {
                int close = line.indexOf("*/", end);
                if (close < 0) {
                    tokens.add(new Token(start, line.length(), "comment.block"));
                    return new LineTokens(tokens, State.BLOCK_COMMENT);
                }
                tokens.add(new Token(start, close + 2, "comment.block"));
                pos = close + 2;
                continue;
            }
            String scope;
            if (matcher.group("string") != null) {
                scope = "string";
            } else if (matcher.group("number") != null) {
                scope = "number";
            } else if (matcher.group("word") != null) {
                scope = scopeOfWord(matcher.group("word"));
            } else {
                scope = "punctuation";
            }
            if (scope != null) {
                tokens.add(new Token(start, end, scope));
            }
            pos = end;
        }
        return new LineTokens(tokens, State.NORMAL);
    }

    private static String scopeOfWord(String word) {
        return switch (word) {
            case "true", "false", "null" -> "constant";
            case "if", "else", "return", "function", "var", "let", "const" -> "keyword";
            default -> null;
        };
    }
}