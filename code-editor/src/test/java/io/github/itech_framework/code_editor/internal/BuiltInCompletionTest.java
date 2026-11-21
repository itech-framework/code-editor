package io.github.itech_framework.code_editor.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.itech_framework.code_editor.completion.CompletionItem;
import io.github.itech_framework.code_editor.completion.CompletionItem.Kind;
import io.github.itech_framework.code_editor.completion.CompletionProvider;
import io.github.itech_framework.code_editor.language.template.TemplateLanguages;

class BuiltInCompletionTest {

    private static final CompletionProvider JS = BuiltInCompletion
            .forLanguage(TemplateLanguages.fromResource("javascript.json"));

    @Test
    void noLanguageGivesNoProvider() {
        assertNull(BuiltInCompletion.forLanguage(null));
    }

    @Test
    void templateLanguageGetsAProvider() {
        assertNotNull(JS);
    }

    @Test
    void providerHasNoTriggerCharacters() {
        assertEquals("", JS.triggerCharacters());
    }

    @Test
    void offersTheKeywordsFromTheTemplate() {
        List<CompletionItem> items = JS.complete("fu", 2);
        assertEquals(1, items.size());
        assertEquals("function", items.get(0).label());
        assertEquals(Kind.KEYWORD, items.get(0).kind());
        assertEquals("return", JS.complete("ret", 3).get(0).label());
    }

    @Test
    void offersKeywordsThenDocumentWords() {
        List<CompletionItem> items = JS.complete("foo fo", 6);
        assertEquals(List.of("for", "foo"), items.stream().map(CompletionItem::label).toList());
        assertEquals(Kind.KEYWORD, items.get(0).kind());
        assertEquals(Kind.WORD, items.get(1).kind());
    }
}