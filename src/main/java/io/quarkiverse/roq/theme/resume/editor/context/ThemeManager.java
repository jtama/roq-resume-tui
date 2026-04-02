package io.quarkiverse.roq.theme.resume.editor.context;

import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;

import dev.tamboui.css.engine.StyleEngine;

@ApplicationScoped
public class ThemeManager {

    private StyleEngine styleEngine;
    private String activeTheme = "catpuccin";

    public void initialize(StyleEngine engine) {
        this.styleEngine = engine;
        try {
            styleEngine.loadStylesheet("catpuccin",
                    Path.of("catpuccin.tcss"));
            //            styleEngine.loadStylesheet("everforest", "everforest.tcss");
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to load stylesheets", e);
        }
        styleEngine.setActiveStylesheet("catpuccin");
    }

    public void toggle() {
        if ("catpuccin".equals(activeTheme)) {
            activeTheme = "everforest";
            styleEngine.setActiveStylesheet("everforest");
        } else {
            activeTheme = "catpuccin";
            styleEngine.setActiveStylesheet("catpuccin");
        }
    }

    public void setTheme(String theme) {
        this.activeTheme = theme;
        styleEngine.setActiveStylesheet(theme);
    }
}