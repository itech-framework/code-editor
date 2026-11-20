package io.github.itech_framework.code_editor;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import io.github.itech_framework.code_editor.internal.FindPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.StackPane;

final class CodeEditorSkin extends SkinBase<CodeEditor> {

    private final VirtualizedScrollPane<CodeArea> scrollPane;

    private final StackPane stack = new StackPane();

    CodeEditorSkin(CodeEditor editor) {
        super(editor);
        this.scrollPane = new VirtualizedScrollPane<>(editor.area());
        FindPanel panel = editor.findPanel();
        StackPane.setAlignment(panel, Pos.TOP_RIGHT);
        StackPane.setMargin(panel, new Insets(8, 24, 0, 0));
        stack.getChildren().addAll(scrollPane, panel);
        getChildren().add(stack);
    }

    @Override
    public void dispose() {
        stack.getChildren().clear();
        getChildren().clear();
        super.dispose();
    }
}