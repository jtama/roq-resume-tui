package io.quarkiverse.roq.theme.resume.editor.context;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyEvent;

import io.quarkiverse.roq.theme.resume.editor.tui.BioEditorWidget;
import io.quarkiverse.roq.theme.resume.editor.tui.ProfileEditorWidget;
import io.quarkiverse.roq.theme.resume.editor.tui.ResumeSelectorWidget;
import io.quarkiverse.roq.theme.resume.editor.tui.SocialEditorWidget;

@ApplicationScoped
public class GlobalKeyHandler {

    @Inject
    AppContext appContext;

    @Inject
    ThemeManager themeManager;

    @Inject
    BioEditorWidget bioEditor;

    @Inject
    ProfileEditorWidget profileEditor;

    @Inject
    SocialEditorWidget socialEditor;

    @Inject
    ResumeSelectorWidget resumeSelector;

    public EventResult handle(KeyEvent key) {
        char c = key.character();
        return switch (c) {
            case 't', 'T' -> handleThemeToggle();
            case 'x' -> handleSave();
            case 'a' -> handleAddDialog();
            case 'e' -> handleEditDialog();
            case 'i' -> handleImport();
            case 'r', 'R', 'b', 'B', 'p', 'P', 's', 'S', 'h', 'H' -> handleTabNavigation(c);
            default -> {
                appContext.resetStatusMessage();
                yield EventResult.UNHANDLED;
            }
        };
    }

    private EventResult handleThemeToggle() {
        themeManager.toggle();
        appContext.setStatusMessage("Theme toggled");
        return EventResult.HANDLED;
    }

    private EventResult handleSave() {
        if (socialEditor.isDialogOpen() || resumeSelector.isDialogOpen()) {
            return EventResult.UNHANDLED;
        }
        Integer selected = appContext.tabsState().selected();
        if (selected != null) {
            switch (selected) {
                case 3 -> socialEditor.save();
                case 2 -> profileEditor.save();
                case 1 -> bioEditor.save();
            }
        }
        appContext.setStatusMessage("Saved successfully!");
        return EventResult.HANDLED;
    }

    private EventResult handleAddDialog() {
        if (appContext.tabsState().selected() == 3 && !socialEditor.isDialogOpen()) {
            socialEditor.openAddDialog();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private EventResult handleEditDialog() {
        if (appContext.tabsState().selected() == 3 && !socialEditor.isDialogOpen()) {
            socialEditor.openEditDialog();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    /// Handle bio import (only available in Bio tab).
    /// Note: This is a global handler that only works if the widget doesn't consume the 'i' key.
    private EventResult handleImport() {
        Integer selected = appContext.tabsState().selected();
        if (selected != null && selected == 1 && !bioEditor.isImportDialogOpen()) {
            appContext.setStatusMessage("Import bio: enter file path or URL, then press 'i' again");
            // TODO: Implement input dialog for file path / URL entry
            // For now, this shows a message. Full implementation requires:
            // 1. An input dialog widget
            // 2. Or integration with system stdin
            bioEditor.openImportDialog();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private EventResult handleTabNavigation(char c) {
        int tabIndex = switch (c) {
            case 'r', 'R' -> 0;
            case 'b', 'B' -> 1;
            case 'p', 'P' -> 2;
            case 's', 'S' -> 3;
            case 'h', 'H' -> 4;
            default -> appContext.tabsState().selected();
        };
        appContext.tabsState().select(tabIndex);
        appContext.resetStatusMessage();
        return EventResult.HANDLED;
    }
}
