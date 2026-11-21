package io.github.itech_framework.code_editor.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.itech_framework.code_editor.completion.CompletionItem;
import io.github.itech_framework.code_editor.completion.CompletionItem.Kind;

class CompletionLogicTest {

    private static List<String> labels(List<CompletionItem> items) {
        return items.stream().map(CompletionItem::label).toList();
    }

    @Test
    void wordCharactersIncludeLettersDigitsUnderscoreAndDollar() {
        assertTrue(CompletionLogic.isWordChar('a'));
        assertTrue(CompletionLogic.isWordChar('Z'));
        assertTrue(CompletionLogic.isWordChar('5'));
        assertTrue(CompletionLogic.isWordChar('_'));
        assertTrue(CompletionLogic.isWordChar('$'));
    }

    @Test
    void wordCharactersExcludeSeparatorsAndControls() {
        assertFalse(CompletionLogic.isWordChar('-'));
        assertFalse(CompletionLogic.isWordChar('.'));
        assertFalse(CompletionLogic.isWordChar(' '));
        assertFalse(CompletionLogic.isWordChar('@'));
        assertFalse(CompletionLogic.isWordChar('\u0000'));
    }

    @Test
    void burmeseLettersAndCombiningMarksAreWordCharacters() {
        assertTrue(CompletionLogic.isWordChar('\u1019'));
        assertTrue(CompletionLogic.isWordChar('\u103B'));
    }

    @Test
    void wordStartRejectsDigitsAndCombiningMarks() {
        assertTrue(CompletionLogic.isWordStart('a'));
        assertTrue(CompletionLogic.isWordStart('_'));
        assertTrue(CompletionLogic.isWordStart('$'));
        assertTrue(CompletionLogic.isWordStart('\u1019'));
        assertFalse(CompletionLogic.isWordStart('9'));
        assertFalse(CompletionLogic.isWordStart('-'));
        assertFalse(CompletionLogic.isWordStart('\u103B'));
    }

    @Test
    void wordStartWalksBack() {
        assertEquals(4, CompletionLogic.wordStart("foo bar", 7));
        assertEquals(0, CompletionLogic.wordStart("foo bar", 3));
        assertEquals(4, CompletionLogic.wordStart("foo bar", 4));
        assertEquals(0, CompletionLogic.wordStart("a_b$c", 5));
        assertEquals(0, CompletionLogic.wordStart("x12", 3));
    }

    @Test
    void wordStartClampsTheCaret() {
        assertEquals(0, CompletionLogic.wordStart("foo bar", -5));
        assertEquals(4, CompletionLogic.wordStart("foo bar", 99));
    }

    @Test
    void wordEndWalksForward() {
        assertEquals(7, CompletionLogic.wordEnd("foo bar", 5));
        assertEquals(3, CompletionLogic.wordEnd("foo bar", 3));
        assertEquals(3, CompletionLogic.wordEnd("foo bar", 0));
    }

    @Test
    void wordEndClampsTheCaret() {
        assertEquals(7, CompletionLogic.wordEnd("foo bar", 99));
        assertEquals(3, CompletionLogic.wordEnd("foo bar", -1));
    }

    @Test
    void prefixLengthCountsTheTrailingWord() {
        assertEquals(3, CompletionLogic.prefixLength("foo"));
        assertEquals(1, CompletionLogic.prefixLength("a b"));
        assertEquals(2, CompletionLogic.prefixLength("$x"));
        assertEquals(1, CompletionLogic.prefixLength("_"));
        assertEquals(2, CompletionLogic.prefixLength("\u1019\u103B"));
    }

    @Test
    void prefixLengthIsZeroWithoutAWordBeforeTheCaret() {
        assertEquals(0, CompletionLogic.prefixLength(""));
        assertEquals(0, CompletionLogic.prefixLength("foo "));
    }

    @Test
    void prefixLengthIsZeroWhenTheWordStartsWithADigit() {
        assertEquals(0, CompletionLogic.prefixLength("12ab"));
        assertEquals(0, CompletionLogic.prefixLength("x.12"));
    }

    @Test
    void caretOutsideTheTextGivesNothing() {
        assertTrue(CompletionLogic.suggest("abc", 4, List.of("abcd")).isEmpty());
        assertTrue(CompletionLogic.suggest("abc", -1, List.of("abcd")).isEmpty());
    }

    @Test
    void noWordBeforeTheCaretGivesNothing() {
        assertTrue(CompletionLogic.suggest("foo ", 4, List.of("foobar")).isEmpty());
        assertTrue(CompletionLogic.suggest("", 0, List.of("foobar")).isEmpty());
    }

    @Test
    void numberLikePrefixGivesNothing() {
        assertTrue(CompletionLogic.suggest("12", 2, List.of("12ab")).isEmpty());
        assertTrue(CompletionLogic.suggest("x 1ab", 5, List.of("1abc")).isEmpty());
    }

    @Test
    void keywordsMatchThePrefixAndCarryTheReplaceRange() {
        List<CompletionItem> items = CompletionLogic.suggest("fu", 2, List.of("function", "for", "fun"));
        assertEquals(List.of("fun", "function"), labels(items));
        for (CompletionItem item : items) {
            assertEquals(Kind.KEYWORD, item.kind());
            assertEquals(0, item.start());
            assertEquals(2, item.end());
            assertEquals(item.label(), item.insertText());
        }
    }

    @Test
    void keywordEqualToThePrefixIsNotOffered() {
        assertEquals(List.of("foreach"), labels(CompletionLogic.suggest("for", 3, List.of("for", "foreach"))));
    }

    @Test
    void keywordMatchingIgnoresCase() {
        List<CompletionItem> items = CompletionLogic.suggest("FU", 2, List.of("function"));
        assertEquals(List.of("function"), labels(items));
        assertEquals("function", items.get(0).insertText());
    }

    @Test
    void keywordWithTheSameCaseAsThePrefixComesFirst() {
        assertEquals(List.of("Fun", "fun"), labels(CompletionLogic.suggest("Fu", 2, List.of("fun", "Fun"))));
    }

    @Test
    void duplicateKeywordsAppearOnce() {
        assertEquals(List.of("foo1"), labels(CompletionLogic.suggest("fo", 2, List.of("foo1", "foo1"))));
    }

    @Test
    void noMatchGivesNothing() {
        assertTrue(CompletionLogic.suggest("zz", 2, List.of("for")).isEmpty());
    }

    @Test
    void keywordsComeBeforeDocumentWords() {
        List<CompletionItem> items = CompletionLogic.suggest("foobar fo", 9, List.of("for"));
        assertEquals(List.of("for", "foobar"), labels(items));
        assertEquals(Kind.KEYWORD, items.get(0).kind());
        assertEquals(Kind.WORD, items.get(1).kind());
        assertEquals(7, items.get(1).start());
        assertEquals(9, items.get(1).end());
    }

    @Test
    void documentWordEqualToAKeywordIsNotOfferedTwice() {
        List<CompletionItem> items = CompletionLogic.suggest("return retur", 12, List.of("return"));
        assertEquals(List.of("return"), labels(items));
        assertEquals(Kind.KEYWORD, items.get(0).kind());
    }

    @Test
    void shortDocumentWordsAreSkipped() {
        assertTrue(CompletionLogic.suggest("ab a", 4, List.of()).isEmpty());
        assertEquals(List.of("abc"), labels(CompletionLogic.suggest("abc a", 5, List.of())));
    }

    @Test
    void documentWordEqualToThePrefixIsSkipped() {
        assertTrue(CompletionLogic.suggest("abc abc", 7, List.of()).isEmpty());
    }

    @Test
    void theWordBeingTypedIsNotOfferedToItselfAndTheRangeStopsAtTheCaret() {
        List<CompletionItem> items = CompletionLogic.suggest("hello help", 8, List.of());
        assertEquals(List.of("hello"), labels(items));
        assertEquals(6, items.get(0).start());
        assertEquals(8, items.get(0).end());
    }

    @Test
    void repeatedWordsAppearOnce() {
        assertEquals(List.of("alpha"), labels(CompletionLogic.suggest("alpha alpha al", 14, List.of())));
    }

    @Test
    void nearerWordsComeFirst() {
        assertEquals(List.of("apricot", "apple"), labels(CompletionLogic.suggest("apple ap apricot", 8, List.of())));
    }

    @Test
    void wordWithTheSameCaseAsThePrefixBeatsANearerWord() {
        assertEquals(List.of("apple", "Apple"), labels(CompletionLogic.suggest("apple Apple ap", 14, List.of())));
    }

    @Test
    void burmeseWordsComplete() {
        List<CompletionItem> items = CompletionLogic.suggest("\u1000\u1001\u1002 \u1000", 5, List.of());
        assertEquals(List.of("\u1000\u1001\u1002"), labels(items));
        assertEquals(4, items.get(0).start());
        assertEquals(5, items.get(0).end());
    }

    @Test
    void keywordResultsAreCapped() {
        List<String> keywords = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            keywords.add(String.format("k%02d", i));
        }
        List<CompletionItem> items = CompletionLogic.suggest("k", 1, keywords);
        assertEquals(CompletionLogic.MAX_ITEMS, items.size());
        assertEquals("k00", items.get(0).label());
        assertEquals("k49", items.get(items.size() - 1).label());
    }

    @Test
    void wordResultsAreCapped() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            text.append(String.format("wd%02d ", i));
        }
        text.append('w');
        List<CompletionItem> items = CompletionLogic.suggest(text.toString(), text.length(), List.of());
        assertEquals(CompletionLogic.MAX_ITEMS, items.size());
        assertEquals("wd59", items.get(0).label());
    }
}