package io.quarkiverse.roq.theme.resume.editor.tui;

import jakarta.enterprise.context.ApplicationScoped;

import dev.tamboui.toolkit.element.Element;

import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.text;

@ApplicationScoped
public class BioEditorWidget {

    public Element render() {

        return panel("Bio Editor", text("Soon").bold().fill()).fill().focusable().id("BioEditorWidget")
                .bottomTitle("Editing functionality coming soon");
    }
}
