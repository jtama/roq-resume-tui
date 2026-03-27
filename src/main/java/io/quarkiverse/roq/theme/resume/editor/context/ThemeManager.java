package io.quarkiverse.roq.theme.resume.editor.context;

import dev.tamboui.css.engine.StyleEngine;

public class ThemeManager {

    private StyleEngine styleEngine;
    private String activeTheme = "catpuccin";

    public void initialize(StyleEngine engine) {
        this.styleEngine = engine;
        try {
            styleEngine.loadStylesheet("catpuccin", "catpuccin.tcss");
            styleEngine.loadStylesheet("everforest", "everforest.tcss");
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to load stylesheets", e);
        }
        styleEngine.setActiveStylesheet("catpuccin");
    }

    public String getActiveTheme() {
        return activeTheme;
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