package io.github.itech_framework.code_editor.internal;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import io.github.itech_framework.code_editor.language.template.TemplateLanguages;

class BuiltInDiagnosticsTest {

    @Test
    void noLanguageGivesNoProvider() {
        assertNull(BuiltInDiagnostics.forLanguage(null));
    }

    @Test
    void jsonGetsTheStrictValidator() {
        assertInstanceOf(JsonDiagnostics.class,
                BuiltInDiagnostics.forLanguage(TemplateLanguages.fromResource("json.json")));
    }

    @Test
    void otherTemplateLanguagesGetTheLexicalCheck() {
        assertInstanceOf(LexicalDiagnostics.class,
                BuiltInDiagnostics.forLanguage(TemplateLanguages.fromResource("javascript.json")));
        assertInstanceOf(LexicalDiagnostics.class,
                BuiltInDiagnostics.forLanguage(TemplateLanguages.fromResource("java.json")));
    }
}