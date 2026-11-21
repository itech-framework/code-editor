package io.github.itech_framework.code_editor;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import io.github.itech_framework.code_editor.internal.CompletionPopup;
import io.github.itech_framework.code_editor.internal.DiagnosticTooltip;
import io.github.itech_framework.code_editor.internal.FindPanel;
import io.github.itech_framework.code_editor.internal.GoToLinePanel;
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
        FindPanel findPanel = editor.findPanel();
        StackPane.setAlignment(findPanel, Pos.TOP_RIGHT);
        StackPane.setMargin(findPanel, new Insets(8, 24, 0, 0));
        GoToLinePanel goToPanel = editor.goToLinePanel();
        StackPane.setAlignment(goToPanel, Pos.TOP_CENTER);
        StackPane.setMargin(goToPanel, new Insets(8, 0, 0, 0));
        DiagnosticTooltip tooltip = editor.diagnosticTooltip();
        StackPane.setAlignment(tooltip, Pos.TOP_LEFT);
        CompletionPopup completionPopup = editor.completionPopup();
        StackPane.setAlignment(completionPopup, Pos.TOP_LEFT);
        stack.getChildren().addAll(scrollPane, findPanel, goToPanel, tooltip, completionPopup);
        getChildren().add(stack);
    }

    @Override
    public void dispose() {
        stack.getChildren().clear();
        getChildren().clear();
        super.dispose();
    }
}