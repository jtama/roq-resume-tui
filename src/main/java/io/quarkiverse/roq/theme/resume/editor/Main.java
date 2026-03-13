package io.quarkiverse.roq.theme.resume.editor;

import java.util.Optional;
import java.util.Scanner;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.TabsElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.widgets.tabs.TabsState;

import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.tabs;
import static dev.tamboui.toolkit.Toolkit.text;

import io.quarkiverse.roq.theme.resume.editor.model.ResumeSummary;
import io.quarkiverse.roq.theme.resume.editor.service.CryptoService;
import io.quarkiverse.roq.theme.resume.editor.service.ResumeRepository;
import io.quarkiverse.roq.theme.resume.editor.service.YamlExportService;
import io.quarkiverse.roq.theme.resume.editor.tui.BioEditorWidget;
import io.quarkiverse.roq.theme.resume.editor.tui.CVManagerWidget;
import io.quarkiverse.roq.theme.resume.editor.tui.ProfileEditorWidget;
import io.quarkiverse.roq.theme.resume.editor.tui.SocialEditorWidget;
import io.quarkus.logging.Log;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Main implements QuarkusApplication {

    @Inject
    CryptoService cryptoService;

    @Inject
    ResumeRepository resumeRepository;

    @Inject
    YamlExportService yamlExportService;

    @Inject
    ProfileEditorWidget profileEditor;

    @Inject
    SocialEditorWidget socialEditor;

    @Inject
    BioEditorWidget bioEditor;

    @Inject
    CVManagerWidget cvManager;

    private TabsState tabsState = new TabsState();
    private TabsElement tabs = tabs("[B]io", "[P]rofile", "[S]ocial").selected(0).focusable().id("nav").divider(" | ")
            .title(" Navigation ").fill().state(tabsState);

    private String statusMessage = "Press 'a' to add, 'x' to save, 't' to toggle theme, 'q' to exit";

    private enum View {
        CV_MANAGER,
        EDITOR
    }

    private View currentView = View.CV_MANAGER;
    private Optional<ResumeSummary> currentResume = Optional.empty();
    private StyleEngine styleEngine;

    @Override
    public int run(String... args) throws Exception {

        // --- Password Prompt Logic ---
        if (!cryptoService.isPasswordSet()) {
            System.out.print("Enter database password: ");
            if (System.console() != null) {
                char[] pass = System.console().readPassword();
                cryptoService.initialize(new String(pass));
            } else {
                Scanner scanner = new Scanner(System.in);
                if (scanner.hasNextLine()) {
                    cryptoService.initialize(scanner.nextLine());
                } else {
                    System.err.println("No password provided.");
                    return 1;
                }
            }
        } else {
            cryptoService.initialize(null);
        }

        // --- TUI Loop ---
        styleEngine = StyleEngine.create();
        styleEngine.loadStylesheet("catpuccin", "catpuccin.tcss");
        styleEngine.loadStylesheet("everforest", "everforest.tcss");
        styleEngine.setActiveStylesheet("catpuccin");
        try (var runner = ToolkitRunner.builder().styleEngine(styleEngine).build()) {
            runner.run(new Supplier<Element>() {
                @Override
                public Element get() {
                    if (currentView == View.CV_MANAGER) {
                        return panel(cvManager.render()).onKeyEvent(key -> {
                            if (key.isConfirm()) {
                                Log.info("CV Selected");
                            }
                            return EventResult.UNHANDLED;
                        });
                    }
                    return createEditorView();
                }
            });
        }

        // --- Shutdown & Save ---
        try {
            cryptoService.saveAndEncrypt();
            System.out.println("Database saved and encrypted.");
        } catch (Exception e) {
            System.err.println("Failed to save database: " + e.getMessage());
        }

        return 0;
    }

    private Element createEditorView() {
        return dock().top(tabs).center(renderContent())
                .bottom(panel("Status", text(statusMessage).green()))
                .bottomHeight(Constraint.length(5)).onKeyEvent(key -> {
                    if (key.isChar('t') || key.isChar('T')) {
                        String current = styleEngine.getActiveStylesheet().orElse("catpuccin");
                        if ("catpuccin".equals(current)) {
                            styleEngine.setActiveStylesheet("everforest");
                        } else {
                            styleEngine.setActiveStylesheet("catpuccin");
                        }
                        return EventResult.HANDLED;
                    }
                    if (key.isChar('B') || key.isChar('b')) {
                        statusMessage = "Press 'a' to add, 'x' to save, 't' to toggle theme, 'q' to exit";
                        tabsState.select(0);
                        return EventResult.HANDLED;
                    }
                    if (key.isChar('P') || key.isChar('p')) {
                        statusMessage = "Press 'a' to add, 'x' to save, 't' to toggle theme, 'q' to exit";
                        tabsState.select(1);
                        return EventResult.HANDLED;
                    }
                    if (key.isChar('S') || key.isChar('s')) {
                        statusMessage = "Press 'a' to add, 'x' to save, 't' to toggle theme, 'q' to exit";
                        tabsState.select(2);
                        return EventResult.HANDLED;
                    }
                    if (key.isChar('x') && !socialEditor.isDialogOpen()) {
                        int tab = tabsState.selected();
                        if (tab == 2)
                            socialEditor.save();
                        else if (tab == 1)
                            profileEditor.save();

                        statusMessage = "Saved successfully!";
                        return EventResult.HANDLED;
                    }
                    if (key.isChar('a') && tabsState.selected() == 2 && !socialEditor.isDialogOpen()) {
                        socialEditor.openAddDialog();
                        return EventResult.HANDLED;
                    }
                    if (key.isChar('e') && tabsState.selected() == 2 && !socialEditor.isDialogOpen()) {
                        socialEditor.openEditDialog();
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;

                });
    }

    private Element renderContent() {
        Integer selected = tabsState.selected();
        int tabIndex = selected != null ? selected : 0;
        return switch (tabIndex) {
            case 0 -> bioEditor.render();
            case 1 -> profileEditor.render();
            case 2 -> socialEditor.render();
            default -> bioEditor.render();
        };
    }

}
