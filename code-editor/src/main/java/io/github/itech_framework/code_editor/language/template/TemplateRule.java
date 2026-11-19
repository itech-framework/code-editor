package io.github.itech_framework.code_editor.language.template;

import java.util.Set;
import java.util.regex.Pattern;

public sealed interface TemplateRule {

    String scope();

    record Match(Pattern pattern, String scope, StateAction action) implements TemplateRule {
    }

    record Words(Set<String> words, String scope, boolean caseSensitive) implements TemplateRule {
    }

    record Block(Pattern begin, Pattern end, String scope) implements TemplateRule {
    }

    record EndOfLine(StateAction action) implements TemplateRule {

        @Override
        public String scope() {
            return null;
        }
    }
}