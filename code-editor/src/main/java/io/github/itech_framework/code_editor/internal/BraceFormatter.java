package io.github.itech_framework.code_editor.internal;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

import io.github.itech_framework.code_editor.TextEdit;
import io.github.itech_framework.code_editor.formatting.CodeFormatter;
import io.github.itech_framework.code_editor.formatting.FormatOptions;
import io.github.itech_framework.code_editor.language.BracketPair;
import io.github.itech_framework.code_editor.language.FormatterSpec;
import io.github.itech_framework.code_editor.language.LineTokens;
import io.github.itech_framework.code_editor.language.Token;
import io.github.itech_framework.code_editor.language.Tokenizer;
import io.github.itech_framework.code_editor.language.TokenizerState;
import io.github.itech_framework.code_editor.language.template.TemplateTokenizer;

public final class BraceFormatter implements CodeFormatter {

    public static final int MAX_TEXT_LENGTH = 1_000_000;

    public static final int MAX_LEVEL = 64;

    private static final byte CODE = 0;

    private static final byte COMMENT = 1;

    private static final byte LITERAL = 2;

    private static final class Nesting {

        private int[] contribs = new int[64];
        private int[] lines = new int[64];
        private int size;
        private int indent;
        private int lineOpen;
        private int lineNumber;

        void startLine(int number) {
            lineNumber = number;
            lineOpen = 0;
        }

        void open() {
            int contrib = lineOpen == 0 ? 1 : 0;
            if (size == contribs.length) {
                contribs = Arrays.copyOf(contribs, size * 2);
                lines = Arrays.copyOf(lines, size * 2);
            }
            contribs[size] = contrib;
            lines[size] = lineNumber;
            size++;
            indent += contrib;
            lineOpen += contrib;
        }

        void close() {
            if (size == 0) {
                return;
            }
            size--;
            int contrib = contribs[size];
            indent -= contrib;
            if (contrib == 1 && lines[size] == lineNumber) {
                lineOpen--;
            }
        }

        int dedent(int count) {
            int sum = 0;
            for (int i = 0; i < count && size - 1 - i >= 0; i++) {
                sum += contribs[size - 1 - i];
            }
            return sum;
        }
    }

    private final Tokenizer tokenizer;

    private final String openers;

    private final String closers;

    private final boolean trim;

    private final boolean finalNewline;

    public BraceFormatter(Tokenizer tokenizer, FormatterSpec spec) {
        this.tokenizer = Objects.requireNonNull(tokenizer, "tokenizer");
        Objects.requireNonNull(spec, "spec");
        StringBuilder open = new StringBuilder();
        StringBuilder close = new StringBuilder();
        for (BracketPair pair : spec.brackets()) {
            open.append(pair.open());
            close.append(pair.close());
        }
        this.openers = open.toString();
        this.closers = close.toString();
        this.trim = spec.trimTrailingWhitespace();
        this.finalNewline = spec.insertFinalNewline();
    }

    @Override
    public List<TextEdit> format(String text, FormatOptions options) {
        if (text.isBlank() || text.length() > MAX_TEXT_LENGTH) {
            return List.of();
        }
        String formatted = render(text, options.indentUnit());
        if (formatted == null) {
            return List.of();
        }
        TextEdit edit = FormatLogic.diff(text, formatted);
        return edit == null ? List.of() : List.of(edit);
    }

    String render(String text, String unit) {
        String[] lines = text.split("\n", -1);
        TokenizerState initial = tokenizer.initialState();
        TokenizerState state = initial;
        Nesting nesting = new Nesting();
        BitSet cases = new BitSet();
        boolean continued = false;
        StringBuilder out = new StringBuilder(text.length() + 64);
        for (int n = 0; n < lines.length; n++) {
            String raw = lines[n];
            String line = raw;
            String suffix = "";
            if (raw.endsWith("\r")) {
                line = raw.substring(0, raw.length() - 1);
                suffix = "\r";
            }
            if (line.length() > TemplateTokenizer.MAX_LINE_LENGTH) {
                return null;
            }
            boolean startsAtRoot = state.equals(initial);
            LineTokens result = tokenizer.tokenize(line, state);
            boolean endsAtRoot = result.endState().equals(initial);
            byte[] kinds = classify(line, result.tokens());
            int first = firstNonBlank(line);
            nesting.startLine(n);
            String output = raw;
            boolean label = false;
            int base = 0;
            if (first == line.length()) {
                if (startsAtRoot && trim) {
                    output = suffix;
                }
            } else if (startsAtRoot) {
                int leading = leadingClosers(line, first, kinds);
                base = Math.max(0, nesting.indent - nesting.dedent(leading));
                label = isCaseLabel(line, first, kinds);
                int extra = cases.get(0, label ? base : base + 1).cardinality();
                int level = base + extra;
                if (!label && leading == 0 && (continued || startsContinuation(line, first, kinds))) {
                    level++;
                }
                String body = line.substring(first);
                if (trim && endsAtRoot) {
                    body = stripTrailing(body);
                }
                output = unit.repeat(Math.min(level, MAX_LEVEL)) + body + suffix;
            }
            for (int i = 0; i < line.length(); i++) {
                if (kinds[i] != CODE) {
                    continue;
                }
                char c = line.charAt(i);
                if (openers.indexOf(c) >= 0) {
                    nesting.open();
                } else if (closers.indexOf(c) >= 0) {
                    nesting.close();
                }
            }
            int last = lastCodeIndex(line, kinds);
            if (last >= 0) {
                continued = endsWithOperator(line, last, kinds);
                if (startsAtRoot && label && line.charAt(last) == ':' && kinds[last] == CODE) {
                    cases.set(base);
                }
            }
            if (cases.length() > nesting.indent + 1) {
                cases.clear(nesting.indent + 1, cases.length());
            }
            state = result.endState();
            out.append(output);
            if (n < lines.length - 1) {
                out.append('\n');
            }
        }
        if (finalNewline && state.equals(initial) && !text.endsWith("\n")) {
            out.append('\n');
        }
        return out.toString();
    }

    private static byte[] classify(String line, List<Token> tokens) {
        byte[] kinds = new byte[line.length()];
        for (Token token : tokens) {
            byte kind = kindOf(token.scope());
            if (kind == CODE) {
                continue;
            }
            int end = Math.min(token.end(), line.length());
            for (int i = Math.max(0, token.start()); i < end; i++) {
                kinds[i] = kind;
            }
        }
        return kinds;
    }

    private static byte kindOf(String scope) {
        if (scope.startsWith("comment")) {
            return COMMENT;
        }
        if (scope.startsWith("string") || scope.startsWith("regexp")) {
            return LITERAL;
        }
        return CODE;
    }

    private static int firstNonBlank(String line) {
        int i = 0;
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
            i++;
        }
        return i;
    }

    private static String stripTrailing(String body) {
        int end = body.length();
        while (end > 0 && Character.isWhitespace(body.charAt(end - 1))) {
            end--;
        }
        return body.substring(0, end);
    }

    private int leadingClosers(String line, int first, byte[] kinds) {
        int count = 0;
        for (int i = first; i < line.length(); i++) {
            char c = line.charAt(i);
            if (kinds[i] == CODE && closers.indexOf(c) >= 0) {
                count++;
            } else if (c != ' ' && c != '\t') {
                break;
            }
        }
        return count;
    }

    private static boolean isCaseLabel(String line, int first, byte[] kinds) {
        if (kinds[first] != CODE) {
            return false;
        }
        int after;
        if (line.startsWith("case", first)) {
            after = first + 4;
        } else if (line.startsWith("default", first)) {
            after = first + 7;
        } else {
            return false;
        }
        return after >= line.length() || !isWordChar(line.charAt(after));
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    private static int lastCodeIndex(String line, byte[] kinds) {
        for (int i = line.length() - 1; i >= 0; i--) {
            char c = line.charAt(i);
            if (c != ' ' && c != '\t' && kinds[i] != COMMENT) {
                return i;
            }
        }
        return -1;
    }

    private static boolean endsWithOperator(String line, int last, byte[] kinds) {
        if (kinds[last] != CODE) {
            return false;
        }
        char c = line.charAt(last);
        boolean doubled = last > 0 && line.charAt(last - 1) == c && kinds[last - 1] == CODE;
        return switch (c) {
            case '+', '-' -> !doubled;
            case '*', '/', '%', '=', '&', '|', '^', '?', '.' -> true;
            case '>' -> last > 0 && line.charAt(last - 1) == '=' && kinds[last - 1] == CODE;
            default -> false;
        };
    }

    private static boolean startsContinuation(String line, int first, byte[] kinds) {
        if (kinds[first] != CODE) {
            return false;
        }
        char c = line.charAt(first);
        char next = first + 1 < line.length() ? line.charAt(first + 1) : ' ';
        return switch (c) {
            case '.' -> Character.isJavaIdentifierStart(next) || next == '#';
            case '?', '*', '%', '^', '=', '&', '|' -> true;
            case ':', '+', '-' -> Character.isWhitespace(next);
            default -> false;
        };
    }
}