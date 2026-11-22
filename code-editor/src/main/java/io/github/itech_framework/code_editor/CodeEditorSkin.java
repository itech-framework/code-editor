package io.github.itech_framework.code_editor;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import io.github.itech_framework.code_editor.internal.CompletionPopup;
import io.github.itech_framework.code_editor.internal.DiagnosticTooltip;
import io.github.itech_framework.code_editor.internal.EditorInternals;
import io.github.itech_framework.code_editor.internal.FindPanel;
import io.github.itech_framework.code_editor.internal.GoToLinePanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.StackPane;

final class CodeEditorSkin extends SkinBase<CodeEditor> {

    private final VirtualizedScrollPane<CodeArea> scrollPane;

    private final StackPane stack = new StackPane();

    private final DiagnosticTooltip tooltip;

    private final CompletionPopup completionPopup;

    CodeEditorSkin(CodeEditor editor) {
        super(editor);
        this.scrollPane = new VirtualizedScrollPane<>(editor.area());
        EditorInternals internals = EditorInternals.of(editor);
        FindPanel findPanel = internals.findPanel();
        StackPane.setAlignment(findPanel, Pos.TOP_RIGHT);
        StackPane.setMargin(findPanel, new Insets(8, 24, 0, 0));
        GoToLinePanel goToPanel = internals.goToLinePanel();
        StackPane.setAlignment(goToPanel, Pos.TOP_CENTER);
        StackPane.setMargin(goToPanel, new Insets(8, 0, 0, 0));
        tooltip = internals.diagnosticTooltip();
        StackPane.setAlignment(tooltip, Pos.TOP_LEFT);
        completionPopup = internals.completionPopup();
        StackPane.setAlignment(completionPopup, Pos.TOP_LEFT);
        stack.getChildren().addAll(scrollPane, findPanel, goToPanel);
        getChildren().add(stack);
        registerChangeListener(editor.singleLineProperty(), property -> {
            applyScrollBars();
            applyHosting();
        });
        applyScrollBars();
        applyHosting();
    }

    private void applyScrollBars() {
        ScrollBarPolicy policy = getSkinnable().isSingleLine() ? ScrollBarPolicy.NEVER : ScrollBarPolicy.AS_NEEDED;
        scrollPane.setHbarPolicy(policy);
        scrollPane.setVbarPolicy(policy);
    }

    private void applyHosting() {
        if (getSkinnable().isSingleLine()) {
            stack.getChildren().removeAll(tooltip, completionPopup);
            return;
        }
        tooltip.dismiss();
        completionPopup.dismiss();
        if (!stack.getChildren().contains(tooltip)) {
            stack.getChildren().add(tooltip);
        }
        if (!stack.getChildren().contains(completionPopup)) {
            stack.getChildren().add(completionPopup);
        }
    }

    @Override
    public void dispose() {
        tooltip.dismiss();
        completionPopup.dismiss();
        stack.getChildren().clear();
        getChildren().clear();
        super.dispose();
    }
}