package io.github.itech_framework.code_editor.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.itech_framework.code_editor.internal.FindLogic.Options;
import io.github.itech_framework.code_editor.internal.FindLogic.Replacement;
import io.github.itech_framework.code_editor.internal.FindLogic.Result;

class FindLogicTest {

    private static final Options CI = new Options(false, false, false);

    private static final Options CS = new Options(true, false, false);

    private static final Options WORD = new Options(false, true, false);

    private static final Options REGEX = new Options(false, false, true);

    private static final Options WORD_REGEX = new Options(false, true, true);

    private static List<String> spans(Result result) {
        return result.matches().stream().map(m -> m[0] + ".." + m[1]).toList();
    }

    private static List<String> spans(String text, String query, Options options) {
        return spans(FindLogic.findAll(text, query, options));
    }

    private static void assertReplacement(Replacement replacement, int start, int end, String text, int count) {
        assertNull(replacement.error());
        assertEquals(start, replacement.start());
        assertEquals(end, replacement.end());
        assertEquals(text, replacement.text());
        assertEquals(count, replacement.count());
    }

    private static void assertNone(Replacement replacement) {
        assertNull(replacement.error());
        assertEquals(0, replacement.count());
        assertEquals("", replacement.text());
    }

    private static void assertFailure(Replacement replacement) {
        assertNotNull(replacement.error());
        assertEquals(0, replacement.count());
    }

    private static List<int[]> sample() {
        return List.of(new int[] { 2, 4 }, new int[] { 10, 12 }, new int[] { 20, 22 });
    }

    @Test
    void emptyQueryFindsNothing() {
        Result empty = FindLogic.findAll("abc", "", CI);
        assertTrue(empty.matches().isEmpty());
        assertNull(empty.error());
        assertFalse(empty.truncated());
        assertTrue(FindLogic.findAll("abc", null, CI).matches().isEmpty());
    }

    @Test
    void caseInsensitiveByDefault() {
        assertEquals(List.of("0..3", "4..7", "8..11"), spans("Foo foo FOO", "foo", CI));
    }

    @Test
    void caseSensitiveWhenAsked() {
        assertEquals(List.of("4..7"), spans("Foo foo FOO", "foo", CS));
    }

    @Test
    void caseInsensitiveMatchingCoversNonAsciiLetters() {
        assertEquals(List.of("0..5", "6..11"), spans("\u00C9cole \u00E9cole", "\u00E9cole", CI));
    }

    @Test
    void literalQueryIsNotARegex() {
        assertEquals(List.of("0..3"), spans("a.b axb", "a.b", CI));
        assertEquals(List.of("1..2"), spans("f(x)", "(", CI));
    }

    @Test
    void regexModeUsesThePattern() {
        assertEquals(List.of("0..3", "4..7"), spans("a.b axb", "a.b", REGEX));
    }

    @Test
    void matchesDoNotOverlap() {
        assertEquals(List.of("0..2", "2..4"), spans("aaaa", "aa", CI));
    }

    @Test
    void matchesCanSpanLines() {
        assertEquals(List.of("1..2"), spans("a\nb", "\n", CI));
    }

    @Test
    void wholeWordSkipsMatchesInsideWords() {
        assertEquals(List.of("0..3", "17..20"), spans("cat concat cat_x cat", "cat", WORD));
    }

    @Test
    void wholeWordTreatsDigitsAsWordCharacters() {
        assertEquals(List.of("5..8"), spans("cat1 cat", "cat", WORD));
    }

    @Test
    void wholeWordTreatsCombiningMarksAsWordCharacters() {
        assertEquals(List.of("4..6"), spans("ab\u103B ab", "ab", WORD));
    }

    @Test
    void wholeWordWorksWithRegex() {
        assertEquals(List.of("0..3", "4..7"), spans("cat cot cut_", "c.t", WORD_REGEX));
    }

    @Test
    void regexAnchorsWorkPerLine() {
        assertEquals(List.of("0..1", "2..3"), spans("a\nab\nba", "^a", REGEX));
        assertEquals(List.of("1..2", "3..4"), spans("ba\na", "a$", REGEX));
    }

    @Test
    void emptyMatchesAreSkipped() {
        assertEquals(List.of("1..3"), spans("axxb", "x*", REGEX));
        assertTrue(spans("abc", "^", REGEX).isEmpty());
    }

    @Test
    void invalidRegexReportsAnError() {
        Result result = FindLogic.findAll("abc", "(", REGEX);
        assertTrue(result.matches().isEmpty());
        assertNotNull(result.error());
        assertFalse(result.truncated());
    }

    @Test
    void exactlyTheLimitIsNotTruncated() {
        Result result = FindLogic.findAll("a".repeat(FindLogic.MAX_MATCHES), "a", CI);
        assertEquals(FindLogic.MAX_MATCHES, result.matches().size());
        assertFalse(result.truncated());
    }

    @Test
    void moreThanTheLimitIsTruncated() {
        Result result = FindLogic.findAll("a".repeat(FindLogic.MAX_MATCHES + 5), "a", CI);
        assertEquals(FindLogic.MAX_MATCHES, result.matches().size());
        assertTrue(result.truncated());
    }

    @Test
    void resultMatchesCannotBeChanged() {
        Result result = FindLogic.findAll("aaa", "a", CI);
        assertThrows(UnsupportedOperationException.class, () -> result.matches().add(new int[] { 0, 1 }));
    }

    @Test
    void replaceAllWithEmptyQueryChangesNothing() {
        assertNone(FindLogic.replaceAll("abc", "", CI, "x"));
        assertNone(FindLogic.replaceAll("abc", null, CI, "x"));
    }

    @Test
    void replaceAllWithoutMatchesChangesNothing() {
        assertNone(FindLogic.replaceAll("abc", "z", CI, "x"));
    }

    @Test
    void replaceAllReplacesOneMatch() {
        assertReplacement(FindLogic.replaceAll("hello world", "world", CI, "there"), 6, 11, "there", 1);
    }

    @Test
    void replaceAllBuildsOneSpanFromTheFirstToTheLastMatch() {
        assertReplacement(FindLogic.replaceAll("a-b-c", "-", CI, "+"), 1, 4, "+b+", 2);
    }

    @Test
    void replaceAllRespectsCase() {
        assertReplacement(FindLogic.replaceAll("hello world", "WORLD", CI, "there"), 6, 11, "there", 1);
        assertNone(FindLogic.replaceAll("hello world", "WORLD", CS, "there"));
    }

    @Test
    void replaceAllTreatsTheReplacementAsLiteralInTextMode() {
        assertReplacement(FindLogic.replaceAll("a b", "a", CI, "$1\\x"), 0, 1, "$1\\x", 1);
    }

    @Test
    void replaceAllExpandsGroupsInRegexMode() {
        assertReplacement(FindLogic.replaceAll("John Smith", "(\\w+) (\\w+)", REGEX, "$2, $1"), 0, 10,
                "Smith, John", 1);
    }

    @Test
    void replaceAllKeepsTheTextBetweenMatches() {
        assertReplacement(FindLogic.replaceAll("x1 y x2", "x(\\d)", REGEX, "<$1>"), 0, 7, "<1> y <2>", 2);
    }

    @Test
    void replaceAllSkipsEmptyMatches() {
        assertReplacement(FindLogic.replaceAll("axb", "x*", REGEX, "-"), 1, 2, "-", 1);
    }

    @Test
    void replaceAllHonoursWholeWord() {
        assertReplacement(FindLogic.replaceAll("cat concat cat", "cat", WORD, "dog"), 0, 14, "dog concat dog", 2);
    }

    @Test
    void replaceAllReportsABadRegex() {
        assertFailure(FindLogic.replaceAll("abc", "(", REGEX, "x"));
    }

    @Test
    void replaceAllReportsABadReplacement() {
        Replacement missingGroup = FindLogic.replaceAll("a", "(a)", REGEX, "$3");
        assertFailure(missingGroup);
        assertTrue(missingGroup.error().startsWith("Invalid replacement: "));
        Replacement danglingEscape = FindLogic.replaceAll("a", "(a)", REGEX, "\\");
        assertFailure(danglingEscape);
        assertTrue(danglingEscape.error().startsWith("Invalid replacement: "));
    }

    @Test
    void replaceOneReplacesTheMatchAtTheRange() {
        assertReplacement(FindLogic.replaceOne("a b a", "a", CI, "X", 4, 5), 4, 5, "X", 1);
        assertReplacement(FindLogic.replaceOne("a b a", "a", CI, "X", 0, 1), 0, 1, "X", 1);
    }

    @Test
    void replaceOneIgnoresARangeThatIsNotAMatch() {
        assertNone(FindLogic.replaceOne("a b a", "a", CI, "X", 2, 3));
    }

    @Test
    void replaceOneIgnoresARangeWithAnotherEnd() {
        assertNone(FindLogic.replaceOne("abab", "ab", CI, "X", 0, 3));
    }

    @Test
    void replaceOneIgnoresBadArguments() {
        assertNone(FindLogic.replaceOne("abc", "", CI, "X", 0, 1));
        assertNone(FindLogic.replaceOne("abc", null, CI, "X", 0, 1));
        assertNone(FindLogic.replaceOne("abc", "a", CI, "X", -1, 1));
        assertNone(FindLogic.replaceOne("abc", "a", CI, "X", 1, 1));
        assertNone(FindLogic.replaceOne("abc", "a", CI, "X", 0, 9));
    }

    @Test
    void replaceOneSeesTheWordBoundaryOutsideTheRange() {
        assertNone(FindLogic.replaceOne("concat cat", "cat", WORD, "X", 3, 6));
        assertReplacement(FindLogic.replaceOne("concat cat", "cat", WORD, "X", 7, 10), 7, 10, "X", 1);
    }

    @Test
    void replaceOneExpandsGroupsInRegexMode() {
        assertReplacement(FindLogic.replaceOne("ab12", "(\\d)(\\d)", REGEX, "$2$1", 2, 4), 2, 4, "21", 1);
    }

    @Test
    void replaceOneDoesNotAnchorAtTheRangeStart() {
        assertNone(FindLogic.replaceOne("ba", "^a", REGEX, "X", 1, 2));
    }

    @Test
    void replaceOneReportsFailures() {
        assertFailure(FindLogic.replaceOne("abc", "(", REGEX, "X", 0, 1));
        Replacement badReplacement = FindLogic.replaceOne("a", "(a)", REGEX, "$9", 0, 1);
        assertFailure(badReplacement);
        assertTrue(badReplacement.error().startsWith("Invalid replacement: "));
    }

    @Test
    void nextIndexFindsTheFirstMatchAtOrAfterTheOffset() {
        List<int[]> matches = sample();
        assertEquals(0, FindLogic.nextIndex(matches, 0));
        assertEquals(0, FindLogic.nextIndex(matches, 2));
        assertEquals(1, FindLogic.nextIndex(matches, 3));
        assertEquals(1, FindLogic.nextIndex(matches, 10));
        assertEquals(2, FindLogic.nextIndex(matches, 11));
        assertEquals(2, FindLogic.nextIndex(matches, 20));
    }

    @Test
    void nextIndexWrapsAround() {
        assertEquals(0, FindLogic.nextIndex(sample(), 21));
        assertEquals(0, FindLogic.nextIndex(List.of(new int[] { 5, 6 }), 6));
    }

    @Test
    void previousIndexFindsTheLastMatchBeforeTheOffset() {
        List<int[]> matches = sample();
        assertEquals(0, FindLogic.previousIndex(matches, 3));
        assertEquals(0, FindLogic.previousIndex(matches, 10));
        assertEquals(1, FindLogic.previousIndex(matches, 11));
        assertEquals(1, FindLogic.previousIndex(matches, 20));
        assertEquals(2, FindLogic.previousIndex(matches, 21));
        assertEquals(2, FindLogic.previousIndex(matches, 100));
    }

    @Test
    void previousIndexWrapsAround() {
        assertEquals(2, FindLogic.previousIndex(sample(), 0));
        assertEquals(2, FindLogic.previousIndex(sample(), 2));
        assertEquals(0, FindLogic.previousIndex(List.of(new int[] { 5, 6 }), 5));
    }

    @Test
    void navigationOnNoMatchesGivesMinusOne() {
        assertEquals(-1, FindLogic.nextIndex(List.of(), 0));
        assertEquals(-1, FindLogic.previousIndex(List.of(), 0));
    }

    @Test
    void indexOfNeedsTheExactRange() {
        List<int[]> matches = sample();
        assertEquals(0, FindLogic.indexOf(matches, 2, 4));
        assertEquals(1, FindLogic.indexOf(matches, 10, 12));
        assertEquals(2, FindLogic.indexOf(matches, 20, 22));
        assertEquals(-1, FindLogic.indexOf(matches, 2, 3));
        assertEquals(-1, FindLogic.indexOf(matches, 3, 4));
        assertEquals(-1, FindLogic.indexOf(matches, 0, 1));
        assertEquals(-1, FindLogic.indexOf(matches, 99, 100));
    }
}