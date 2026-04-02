package io.quarkiverse.roq.theme.resume.editor.context;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.GenericWidgetElement;
import dev.tamboui.widgets.error.ErrorDisplay;
import dev.tamboui.widgets.tabs.TabsState;

import static dev.tamboui.toolkit.Toolkit.text;

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
    private Exception exception;

    public TabsState tabsState() {
        return tabsState;
    }

    public void setStatusMessage(String message) {
        this.statusMessage = message;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public void resetStatusMessage() {
        this.statusMessage = "Press 'a' to add, 'x' to save, 't' to toggle theme, 'q' to exit";
    }

    public boolean isDirty() {
        return profileEditor.isDirty() || socialEditor.isDirty() || bioEditor.isDirty();
    }

    public boolean isError() {
        return profileEditor.isError() || socialEditor.isError() || bioEditor.isError();
    }

    public Element displayStatus() {
        if (isError()) {
            return GenericWidgetElement.of(ErrorDisplay.builder()
                    .error(exception)
                    .title(" ERROR ")
                    .footer(" Press 'q' to quit, arrows to scroll ")
                    .build());
        }
        if (isDirty() && statusMessage.startsWith("Press")) {
            return text("*UNSAVED CHANGES* " + statusMessage).addClass("warning");
        }
        return text(statusMessage).green();
    }
}