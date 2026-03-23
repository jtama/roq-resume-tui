package io.quarkiverse.roq.theme.resume.editor.tui;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.enterprise.context.ApplicationScoped;

import dev.tamboui.toolkit.element.Element;

import static dev.tamboui.toolkit.Toolkit.markupTextArea;
import static dev.tamboui.toolkit.Toolkit.panel;

@ApplicationScoped
public class HelpWidget {

    private String helpContent;

    public HelpWidget() {
        try {
            // Load the bbcode help file from classpath
            InputStream is = getClass().getResourceAsStream("/help.bbcode");
            if (is != null) {
                helpContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } else {
                helpContent = "[bold]Aide introuvable[/bold]\nLe fichier help.bbcode n'a pas pu être chargé.";
            }
        } catch (Exception e) {
            helpContent = "[bold]Erreur de chargement[/bold]\nUne erreur est survenue lors de la lecture du fichier d'aide : "
                    + e.getMessage();
        }
    }

    public Element render() {
        // We use markupTextArea as requested, wrapped in a panel for styling/layout consistency
        return panel("Aide & Raccourcis", markupTextArea(helpContent)
                .wrapWord()
                .rounded()
                .fill());
    }
}
