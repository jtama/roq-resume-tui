package io.quarkiverse.roq.theme.resume.editor.tui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

import dev.tamboui.toolkit.element.Element;
import io.quarkiverse.roq.theme.resume.editor.model.Bio;

import static dev.tamboui.toolkit.Toolkit.button;
import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.text;

@ApplicationScoped
public class BioEditorWidget {

    @Inject
    BioEditor bioEditor;

    private boolean isEditing = false;

    public Element render() {
        if (bioEditor.getCurrentBio() == null) {
            bioEditor.load();
        }

        if (!isEditing) {
            return panel("Bio", 
                    column(
                        renderTree(bioEditor.getCurrentBio()),
                        button("Edit").onClick(() -> isEditing = true)
                    )).fill();
        } else {
            return panel("Bio", 
                    column(
                        renderForm(bioEditor.getCurrentBio()),
                        button("Save").onClick(() -> {
                            isEditing = false;
                            bioEditor.save();
                        })
                    )).fill();
        }
    }

    private Element renderTree(Bio bio) {
        // Simple tree rendering
        return column(
            bio.list().stream()
                .map(section -> column(
                    text(section.title()).bold(),
                    renderItems(section.items(), 2)
                ))
                .toArray(Element[]::new)
        );
    }

    private Element renderItems(List<Bio.Item> items, int indent) {
        if (items == null) return text("");
        return column(
            items.stream()
                .map(item -> column(
                    text(" ".repeat(indent) + "- " + item.title()),
                    renderItems(item.subItems(), indent + 2)
                ))
                .toArray(Element[]::new)
        );
    }

    private Element renderForm(Bio bio) {
        // Simplified form: List of sections, each with a text field for title
        return column(
            bio.list().stream()
                .map(section -> column(
                    text("Section: " + section.title()).bold(),
                    // In a real form, this would be a TextField bound to the BioEditor's state
                    text("Editable fields not fully implemented for recursive structure")
                ))
                .toArray(Element[]::new)
        );
    }
}
