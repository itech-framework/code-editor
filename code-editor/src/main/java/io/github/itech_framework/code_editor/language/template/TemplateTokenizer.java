package io.github.itech_framework.code_editor.language.template;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.itech_framework.code_editor.language.LineTokens;
import io.github.itech_framework.code_editor.language.Token;
import io.github.itech_framework.code_editor.language.Tokenizer;
import io.github.itech_framework.code_editor.language.TokenizerState;

public final class TemplateTokenizer implements Tokenizer {

    public static final int MAX_LINE_LENGTH = 10_000;
    public static final int MAX_STACK_DEPTH = 64;
    public static final long LINE_BUDGET_NANOS = 50_000_000L;

    private final LanguageTemplate template;
    private final TemplateState initial;

    public TemplateTokenizer(LanguageTemplate template) {
        this.template = Objects.requireNonNull(template, "template");
        this.initial = new TemplateState(List.of(template.startState()), null);
    }

    @Override
    public TokenizerState initialState() {
        return initial;
    }

    @Override
    public LineTokens tokenize(String line, TokenizerState state) {
        TemplateState start = initial;
        if (state instanceof TemplateState known && accepts(known)) {
            start = known;
        }
        if (line.length() > MAX_LINE_LENGTH) {
            return new LineTokens(List.of(), start);
        }
        try {
            return run(line, start);
        } catch (BudgetExceeded ex) {
            return new LineTokens(List.of(), start);
        }
    }

    private boolean accepts(TemplateState state) {
        for (String name : state.stack()) {
            if (!template.states().containsKey(name)) {
                return false;
            }
        }
        return true;
    }

    private LineTokens run(String line, TemplateState start) {
        CharSequence text = new BudgetedText(line, System.nanoTime() + LINE_BUDGET_NANOS);
        int length = line.length();
        List<String> stack = new ArrayList<>(start.stack());
        TemplateRule.Block block = start.block();
        List<Token> tokens = new ArrayList<>();
        int pos = 0;
        int segmentStart = 0;
        int eolSteps = 0;

        while (true) {
            if (block != null) {
                Matcher end = matcher(block.end(), text, pos);
                if (end.find()) {
                    add(tokens, segmentStart, end.end(), block.scope());
                    pos = end.end();
                    block = null;
                    continue;
                }
                add(tokens, segmentStart, length, block.scope());
                break;
            }

            List<TemplateRule> rules = template.states().get(stack.get(stack.size() - 1));

            if (pos >= length) {
                TemplateRule.EndOfLine eol = findEndOfLine(rules);
                if (eol == null || eolSteps++ >= MAX_STACK_DEPTH || !apply(eol.action(), stack)) {
                    break;
                }
                continue;
            }

            boolean matched = false;
            for (TemplateRule rule : rules) {
                if (rule instanceof TemplateRule.Match match) {
                    Matcher m = matcher(match.pattern(), text, pos);
                    if (m.lookingAt() && m.end() > pos) {
                        add(tokens, pos, m.end(), match.scope());
                        apply(match.action(), stack);
                        pos = m.end();
                        matched = true;
                    }
                } else if (rule instanceof TemplateRule.Words words) {
                    int wordEnd = wordEnd(line, pos);
                    if (wordEnd > pos && words.words().contains(key(line.substring(pos, wordEnd), words.caseSensitive()))) {
                        add(tokens, pos, wordEnd, words.scope());
                        pos = wordEnd;
                        matched = true;
                    }
                } else if (rule instanceof TemplateRule.Block opening) {
                    Matcher m = matcher(opening.begin(), text, pos);
                    if (m.lookingAt() && m.end() > pos) {
                        segmentStart = pos;
                        pos = m.end();
                        block = opening;
                        matched = true;
                    }
                }
                if (matched) {
                    break;
                }
            }

            if (!matched) {
                int wordEnd = wordEnd(line, pos);
                pos = wordEnd > pos ? wordEnd : pos + Character.charCount(line.codePointAt(pos));
            }
        }

        return new LineTokens(tokens, new TemplateState(stack, block));
    }

    private static TemplateRule.EndOfLine findEndOfLine(List<TemplateRule> rules) {
        for (TemplateRule rule : rules) {
            if (rule instanceof TemplateRule.EndOfLine eol) {
                return eol;
            }
        }
        return null;
    }

    private static boolean apply(StateAction action, List<String> stack) {
        int last = stack.size() - 1;
        switch (action.type()) {
            case NEXT:
                if (stack.get(last).equals(action.target())) {
                    return false;
                }
                stack.set(last, action.target());
                return true;
            case PUSH:
                if (stack.size() >= MAX_STACK_DEPTH) {
                    return false;
                }
                stack.add(action.target());
                return true;
            case POP:
                if (stack.size() <= 1) {
                    return false;
                }
                stack.remove(last);
                return true;
            default:
                return false;
        }
    }

    private static void add(List<Token> tokens, int start, int end, String scope) {
        if (end > start) {
            tokens.add(new Token(start, end, scope));
        }
    }

    private static String key(String word, boolean caseSensitive) {
        return caseSensitive ? word : word.toLowerCase(Locale.ROOT);
    }

    private static Matcher matcher(Pattern pattern, CharSequence text, int from) {
        Matcher matcher = pattern.matcher(text);
        matcher.region(from, text.length());
        matcher.useTransparentBounds(true);
        matcher.useAnchoringBounds(false);
        return matcher;
    }

    private static int wordEnd(String line, int pos) {
        if (pos > 0 && isWordChar(line.charAt(pos - 1))) {
            return pos;
        }
        int end = pos;
        while (end < line.length() && isWordChar(line.charAt(end))) {
            end++;
        }
        return end;
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    private static final class BudgetedText implements CharSequence {

        private final String text;
        private final long deadline;
        private int calls;

        private BudgetedText(String text, long deadline) {
            this.text = text;
            this.deadline = deadline;
        }

        @Override
        public int length() {
            return text.length();
        }

        @Override
        public char charAt(int index) {
            if ((++calls & 1023) == 0 && System.nanoTime() > deadline) {
                throw new BudgetExceeded();
            }
            return text.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return text.subSequence(start, end);
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private static final class BudgetExceeded extends RuntimeException {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private BudgetExceeded() {
            super(null, null, false, false);
        }
    }
}