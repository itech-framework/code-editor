package io.github.itech_framework.code_editor;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import javafx.scene.control.SkinBase;

final class CodeEditorSkin extends SkinBase<CodeEditor> {

    private final VirtualizedScrollPane<CodeArea> scrollPane;

    CodeEditorSkin(CodeEditor editor) {
        super(editor);
        this.scrollPane = new VirtualizedScrollPane<>(editor.area());
        getChildren().add(scrollPane);
    }

    @Override
    public void dispose() {
        getChildren().clear();
        super.dispose();
    }
}