package io.github.itech_framework.code_editor.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import io.github.itech_framework.code_editor.language.LineTokens;
import io.github.itech_framework.code_editor.language.Tokenizer;
import io.github.itech_framework.code_editor.language.TokenizerState;

class HighlightCacheTest {

    private enum State implements TokenizerState {
        ROOT, COMMENT
    }

    private static final Tokenizer TOKENIZER = new Tokenizer() {

        @Override
        public TokenizerState initialState() {
            return State.ROOT;
        }

        @Override
        public LineTokens tokenize(String line, TokenizerState state) {
            boolean open = state == State.COMMENT;
            int i = 0;
            while (i < line.length()) {
                if (open) {
                    int end = line.indexOf("*/", i);
                    if (end < 0) {
                        i = line.length();
                    } else {
                        open = false;
                        i = end + 2;
                    }
                } else {
                    int start = line.indexOf("/*", i);
                    if (start < 0) {
                        i = line.length();
                    } else {
                        open = true;
                        i = start + 2;
                    }
                }
            }
            return new LineTokens(List.of(), open ? State.COMMENT : State.ROOT);
        }
    };

    private static List<String> lines(String text) {
        return Arrays.asList(text.split("\n", -1));
    }

    private static int breaks(String text) {
        int total = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                total++;
            }
        }
        return total;
    }

    private static int settle(HighlightCache cache, String text, int window) {
        List<String> all = lines(text);
        if (cache.size() != all.size()) {
            cache.reset(all.size());
        }
        int plans = 0;
        while (true) {
            HighlightCache.Plan plan = cache.plan(window);
            if (plan == null) {
                return plans;
            }
            plans++;
            List<String> part = new ArrayList<>(all.subList(plan.first(), plan.end()));
            TokenizerState start = plan.previous() == null ? TOKENIZER.initialState() : plan.previous().end();
            HighlightRun.Result result = HighlightRun.run(TOKENIZER, part, start, plan.window(), plan.lastDirty(),
                    plan.more(), 20_000, () -> false);
            cache.apply(plan.first(), result.infos(), result.incomplete());
        }
    }

    private static List<HighlightRun.LineInfo> expected(String text) {
        List<HighlightRun.LineInfo> result = new ArrayList<>();
        TokenizerState state = TOKENIZER.initialState();
        for (String line : lines(text)) {
            TokenizerState end = TOKENIZER.tokenize(line, state).endState();
            result.add(new HighlightRun.LineInfo(state, end));
            state = end;
        }
        return result;
    }

    private static void assertMatchesFullRun(HighlightCache cache, String text) {
        List<HighlightRun.LineInfo> want = expected(text);
        assertEquals(want.size(), cache.size());
        for (int i = 0; i < want.size(); i++) {
            assertEquals(want.get(i), cache.get(i), "line " + i);
        }
    }

    @Test
    void resetMarksEveryLineDirty() {
        HighlightCache cache = new HighlightCache();
        cache.reset(3);
        HighlightCache.Plan plan = cache.plan(10);
        assertNotNull(plan);
        assertEquals(0, plan.first());
        assertEquals(3, plan.end());
        assertEquals(2, plan.lastDirty());
        assertFalse(plan.more());
        assertNull(plan.previous());
    }

    @Test
    void cleanCacheHasNoPlan() {
        HighlightCache cache = new HighlightCache();
        settle(cache, "a\nb\nc", 10);
        assertNull(cache.plan(10));
    }

    @Test
    void editMarksOnlyTheEditedLine() {
        HighlightCache cache = new HighlightCache();
        settle(cache, "a\nb\nc\nd\ne", 1);
        cache.edit(2, 0, 0);
        HighlightCache.Plan plan = cache.plan(1);
        assertEquals(2, plan.first());
        assertEquals(4, plan.end());
        assertEquals(0, plan.lastDirty());
        assertTrue(plan.more());
        assertNotNull(plan.previous());
    }

    @Test
    void insertedLinesAreDirty() {
        HighlightCache cache = new HighlightCache();
        settle(cache, "a\nb\nc", 10);
        cache.edit(1, 0, 2);
        assertEquals(5, cache.size());
        HighlightCache.Plan plan = cache.plan(10);
        assertEquals(1, plan.first());
        assertEquals(2, plan.lastDirty());
    }

    @Test
    void removedLinesShrinkTheCache() {
        HighlightCache cache = new HighlightCache();
        settle(cache, "a\nb\nc\nd\ne", 10);
        cache.edit(1, 2, 0);
        assertEquals(3, cache.size());
        HighlightCache.Plan plan = cache.plan(10);
        assertEquals(1, plan.first());
        assertEquals(0, plan.lastDirty());
    }

    @Test
    void unclosedCommentIsProcessedInChunks() {
        HighlightCache cache = new HighlightCache();
        settle(cache, "x\na\nb\nc\nd\ne\nf", 2);
        cache.edit(0, 0, 0);
        int plans = settle(cache, "/*\na\nb\nc\nd\ne\nf", 2);
        assertEquals(3, plans);
        assertMatchesFullRun(cache, "/*\na\nb\nc\nd\ne\nf");
    }

    @Test
    void closingACommentResyncsAndStops() {
        HighlightCache cache = new HighlightCache();
        settle(cache, "/*\na\n*/\nb\nc\nd", 2);
        cache.edit(2, 0, 0);
        settle(cache, "/*\na\nx\nb\nc\nd", 2);
        assertMatchesFullRun(cache, "/*\na\nx\nb\nc\nd");
    }

    @Test
    void cancelledRunReturnsNull() {
        List<HighlightRun.LineInfo> old = new ArrayList<>(Collections.singletonList((HighlightRun.LineInfo) null));
        assertNull(HighlightRun.run(TOKENIZER, List.of("a"), State.ROOT, old, 0, false, 20_000, () -> true));
    }

    @Test
    void overlongLinesAreNotTokenized() {
        List<HighlightRun.LineInfo> old = new ArrayList<>(Collections.singletonList((HighlightRun.LineInfo) null));
        HighlightRun.Result result = HighlightRun.run(TOKENIZER, List.of("/*" + "x".repeat(30)), State.ROOT, old, 0,
                false, 10, () -> false);
        assertEquals(State.ROOT, result.infos().get(0).end());
    }

    @Test
    void matchesFullTokenizationAfterRandomEdits() {
        Random random = new Random(7);
        String[] fragments = { "/*", "*/", "\n", "\n", "x", " ", "/", "*", "a b" };
        for (int round = 0; round < 300; round++) {
            StringBuilder builder = new StringBuilder();
            int length = random.nextInt(30);
            for (int i = 0; i < length; i++) {
                builder.append(fragments[random.nextInt(fragments.length)]);
            }
            String text = builder.toString();
            int window = 1 + random.nextInt(5);
            HighlightCache cache = new HighlightCache();
            settle(cache, text, window);
            for (int step = 0; step < 20; step++) {
                int position = random.nextInt(text.length() + 1);
                int removeLength = random.nextInt(4) == 0 ? Math.min(text.length() - position, random.nextInt(8)) : 0;
                String inserted = random.nextBoolean() ? fragments[random.nextInt(fragments.length)] : "";
                String removed = text.substring(position, position + removeLength);
                int line = breaks(text.substring(0, position));
                text = text.substring(0, position) + inserted + text.substring(position + removeLength);
                cache.edit(line, breaks(removed), breaks(inserted));
                if (random.nextInt(4) != 0) {
                    settle(cache, text, window);
                    assertMatchesFullRun(cache, text);
                }
            }
        }
    }
}