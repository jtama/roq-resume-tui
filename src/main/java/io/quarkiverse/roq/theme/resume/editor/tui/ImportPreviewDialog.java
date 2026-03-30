package io.quarkiverse.roq.theme.resume.editor.tui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.inject.Singleton;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.TreeElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.widgets.tree.TreeNode;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dialog;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.text;
import static dev.tamboui.toolkit.Toolkit.tree;

import io.quarkiverse.roq.theme.resume.editor.model.Bio;

/// Dialog widget for previewing imported Bio YAML data before confirmation.
/// Shows a tree view of the Bio structure. User can press Enter to confirm or Escape to cancel.
/// The dialog is modal and blocks interaction with the parent widget until dismissed.
@Singleton
public class ImportPreviewDialog {

    private final TreeElement<Object> previewTree = tree().id("importPreview").focusable();
    private boolean isOpen = false;
    private Bio previewData = null;
    private Runnable onConfirm = () -> {
    };
    private Runnable onCancel = () -> {
    };

    /// Check if the dialog is currently open.
    public boolean isOpen() {
        return isOpen;
    }

    /// Open the dialog with preview data and callbacks.
    /// @param bio the Bio data to preview
    /// @param onConfirm callback when user confirms import (Enter key)
    /// @param onCancel callback when user cancels import (Escape key)
    public void open(Bio bio, Runnable onConfirm, Runnable onCancel) {
        this.previewData = bio;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        this.isOpen = true;
        updatePreviewTree();
    }

    /// Close the dialog.
    public void close() {
        isOpen = false;
        previewData = null;
    }

    /// Render the dialog. Returns a dialog element if open, otherwise renders an empty panel.
    public Element render() {
        if (!isOpen || previewData == null) {
            return panel(""); // Empty panel when not open
        }

        // @formatter:off
        return dialog("Bio Import Preview (Press Enter to confirm, Escape to cancel)",
                column(
                        text("Preview of data to be imported:"),
                        panel("", previewTree).fill()
                )
                        .fill()
                )
                .length(500)
                .width(800)
                .onKeyEvent(key -> {
                    if (key.isConfirm()) {
                        handleConfirm();
                        return EventResult.HANDLED;
                    }
                    if (key.code() == KeyCode.ESCAPE) {
                        handleCancel();
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                });
        // @formatter:on
    }

    /// Update the preview tree with Bio sections and items.
    private void updatePreviewTree() {
        // Reset tree view - we'll rebuild it from scratch
        List<TreeNode<Object>> roots = new ArrayList<>();

        if (previewData == null || previewData.list() == null) {
            previewTree.roots(new TreeNode[0]);
            return;
        }

        for (Bio.Section section : previewData.list()) {
            String sectionTitle = "[" + Objects.requireNonNullElse(section.title(), "Untitled") + "]";
            TreeNode<Object> sectionNode = TreeNode.of(sectionTitle, section);

            if (section.items() != null) {
                buildItemNodes(sectionNode, section.items());
            } else {
                sectionNode.leaf();
            }

            roots.add(sectionNode);
        }

        previewTree.roots(roots.toArray(new TreeNode[0]));
    }

    /// Recursively build item nodes for the tree.
    private void buildItemNodes(TreeNode<Object> parent, List<Bio.Item> items) {
        for (Bio.Item item : items) {
            String itemLabel = Objects.requireNonNullElse(item.title(), "Unnamed");
            TreeNode<Object> itemNode = TreeNode.of(itemLabel, item);

            if (item.subItems() != null && !item.subItems().isEmpty()) {
                buildItemNodes(itemNode, item.subItems());
            } else {
                itemNode.leaf();
            }

            parent.add(itemNode);
        }
    }

    /// Handle the confirm (Enter key).
    private void handleConfirm() {
        onConfirm.run();
        close();
    }

    /// Handle the cancel (Escape key).
    private void handleCancel() {
        onCancel.run();
        close();
    }
}
