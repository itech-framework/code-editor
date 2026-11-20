package io.github.itech_framework.code_editor.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class FindLogic {

    public static final int MAX_MATCHES = 1_000;

    private static final long BUDGET_NANOS = 300_000_000L;

    private static final String WORD = "[\\p{L}\\p{N}\\p{M}_]";

    public record Options(boolean caseSensitive, boolean wholeWord, boolean regex) {
    }

    public record Result(List<int[]> matches, String error, boolean truncated) {

        public Result {
            matches = List.copyOf(matches);
        }
    }

    private static final class BudgetExceeded extends RuntimeException {

        /**
		 * 
		 */
		private static final long serialVersionUID = -5017641129506990049L;

		BudgetExceeded() {
            super(null, null, false, false);
        }
    }

    private static final class Budget implements CharSequence {

        private final CharSequence text;
        private final long deadline;
        private int reads;

        Budget(CharSequence text) {
            this.text = text;
            this.deadline = System.nanoTime() + BUDGET_NANOS;
        }

        @Override
        public int length() {
            return text.length();
        }

        @Override
        public char charAt(int index) {
            if ((++reads & 1023) == 0 && System.nanoTime() > deadline) {
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
            return text.toString();
        }
    }
    
    public record Replacement(int start, int end, String text, int count, String error) {

        static Replacement none() {
            return new Replacement(0, 0, "", 0, null);
        }

        static Replacement failure(String error) {
            return new Replacement(0, 0, "", 0, error);
        }
    }

    private FindLogic() {
    }

    public static Result findAll(CharSequence text, String query, Options options) {
        if (query == null || query.isEmpty()) {
            return new Result(List.of(), null, false);
        }
        Pattern pattern;
        try {
            pattern = compile(query, options);
        } catch (PatternSyntaxException ex) {
            return new Result(List.of(), ex.getDescription(), false);
        }
        List<int[]> matches = new ArrayList<>();
        boolean truncated = false;
        try {
            Matcher matcher = pattern.matcher(new Budget(text));
            while (matcher.find()) {
                if (matcher.end() == matcher.start()) {
                    continue;
                }
                if (matches.size() == MAX_MATCHES) {
                    truncated = true;
                    break;
                }
                matches.add(new int[] { matcher.start(), matcher.end() });
            }
        } catch (BudgetExceeded ex) {
            return new Result(List.of(), "Search took too long", false);
        }
        return new Result(matches, null, truncated);
    }
    
    public static Replacement replaceAll(CharSequence text, String query, Options options, String replacement) {
        if (query == null || query.isEmpty()) {
            return Replacement.none();
        }
        Pattern pattern;
        try {
            pattern = compile(query, options);
        } catch (PatternSyntaxException ex) {
            return Replacement.failure(ex.getDescription());
        }
        String template = options.regex() ? replacement : Matcher.quoteReplacement(replacement);
        try {
            Matcher matcher = pattern.matcher(new Budget(text));
            StringBuilder out = new StringBuilder();
            int first = -1;
            int last = -1;
            int count = 0;
            while (matcher.find()) {
                if (matcher.end() == matcher.start()) {
                    continue;
                }
                if (first < 0) {
                    first = matcher.start();
                }
                last = matcher.end();
                matcher.appendReplacement(out, template);
                count++;
            }
            if (count == 0) {
                return Replacement.none();
            }
            matcher.appendTail(out);
            String span = out.substring(first, out.length() - (text.length() - last));
            return new Replacement(first, last, span, count, null);
        } catch (BudgetExceeded ex) {
            return Replacement.failure("Search took too long");
        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
            return Replacement.failure("Invalid replacement: " + ex.getMessage());
        }
    }

    public static Replacement replaceOne(CharSequence text, String query, Options options, String replacement,
            int start, int end) {
        if (query == null || query.isEmpty() || start < 0 || end <= start || end > text.length()) {
            return Replacement.none();
        }
        Pattern pattern;
        try {
            pattern = compile(query, options);
        } catch (PatternSyntaxException ex) {
            return Replacement.failure(ex.getDescription());
        }
        String template = options.regex() ? replacement : Matcher.quoteReplacement(replacement);
        try {
            Matcher matcher = pattern.matcher(new Budget(text));
            matcher.useTransparentBounds(true);
            matcher.useAnchoringBounds(false);
            matcher.region(start, text.length());
            if (!matcher.lookingAt() || matcher.end() != end) {
                return Replacement.none();
            }
            StringBuilder out = new StringBuilder();
            matcher.appendReplacement(out, template);
            return new Replacement(start, end, out.substring(start), 1, null);
        } catch (BudgetExceeded ex) {
            return Replacement.failure("Search took too long");
        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
            return Replacement.failure("Invalid replacement: " + ex.getMessage());
        }
    }

    public static int nextIndex(List<int[]> matches, int offset) {
        if (matches.isEmpty()) {
            return -1;
        }
        int index = firstStartAtOrAfter(matches, offset);
        return index == matches.size() ? 0 : index;
    }

    public static int previousIndex(List<int[]> matches, int offset) {
        if (matches.isEmpty()) {
            return -1;
        }
        int index = firstStartAtOrAfter(matches, offset) - 1;
        return index < 0 ? matches.size() - 1 : index;
    }

    public static int indexOf(List<int[]> matches, int start, int end) {
        int index = firstStartAtOrAfter(matches, start);
        if (index < matches.size() && matches.get(index)[0] == start && matches.get(index)[1] == end) {
            return index;
        }
        return -1;
    }

    private static int firstStartAtOrAfter(List<int[]> matches, int offset) {
        int low = 0;
        int high = matches.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (matches.get(middle)[0] < offset) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    private static Pattern compile(String query, Options options) {
        String body = options.regex() ? query : Pattern.quote(query);
        if (options.wholeWord()) {
            body = "(?<!" + WORD + ")(?:" + body + ")(?!" + WORD + ")";
        }
        int flags = Pattern.MULTILINE;
        if (!options.caseSensitive()) {
            flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        return Pattern.compile(body, flags);
    }
}