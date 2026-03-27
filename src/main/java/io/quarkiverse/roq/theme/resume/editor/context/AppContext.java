package io.quarkiverse.roq.theme.resume.editor.context;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.tamboui.widgets.tabs.TabsState;

import io.quarkiverse.roq.theme.resume.editor.tui.BioEditorWidget;
import io.quarkiverse.roq.theme.resume.editor.tui.ProfileEditorWidget;
import io.quarkiverse.roq.theme.resume.editor.tui.SocialEditorWidget;

@ApplicationScoped
public class AppContext {

    @Inject
    ProfileEditorWidget profileEditor;

    @Inject
    SocialEditorWidget socialEditor;

    @Inject
    BioEditorWidget bioEditor;

    private final TabsState tabsState = new TabsState(0);
    private String statusMessage = "Press 'a' to add, 'x' to save, 't' to toggle theme, 'q' to exit";

    public TabsState tabsState() {
        return tabsState;
    }

    public String statusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String message) {
        this.statusMessage = message;
    }

    public void resetStatusMessage() {
        this.statusMessage = "Press 'a' to add, 'x' to save, 't' to toggle theme, 'q' to exit";
    }

    public boolean isDirty() {
        return profileEditor.isDirty() || socialEditor.isDirty() || bioEditor.isDirty();
    }

    public String displayStatus() {
        String display = statusMessage;
        if (isDirty() && display.startsWith("Press")) {
            display = "*UNSAVED CHANGES* " + display;
        }
        return display;
    }
}