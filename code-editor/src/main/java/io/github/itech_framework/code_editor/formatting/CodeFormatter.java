package io.github.itech_framework.code_editor.formatting;

import java.util.List;

import io.github.itech_framework.code_editor.TextEdit;

@FunctionalInterface
public interface CodeFormatter {

    List<TextEdit> format(String text, FormatOptions options);
}