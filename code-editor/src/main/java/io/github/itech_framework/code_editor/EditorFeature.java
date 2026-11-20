package io.github.itech_framework.code_editor;

public interface EditorFeature {

    String getId();

    void install(CodeEditor editor);

    void uninstall(CodeEditor editor);
}