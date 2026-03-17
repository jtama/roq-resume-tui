package io.quarkiverse.roq.theme.resume.editor;

import java.util.Scanner;

import jakarta.inject.Inject;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.TabsElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.widgets.tabs.TabsState;

import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.tabs;
import static dev.tamboui.toolkit.Toolkit.text;

import io.quarkiverse.roq.theme.resume.editor.service.CryptoService;
import io.quarkiverse.roq.theme.resume.editor.service.ResumeRepository;
import io.quarkiverse.roq.theme.resume.editor.service.YamlExportService;
import io.quarkiverse.roq.theme.resume.editor.tui.BioEditorWidget;
import io.quarkiverse.roq.theme.resume.editor.tui.ProfileEditorWidget;
import io.quarkiverse.roq.theme.resume.editor.tui.ResumeSelectorWidget;
import io.quarkiverse.roq.theme.resume.editor.tui.SocialEditorWidget;
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
    ResumeSelectorWidget resumeSelector;

    private TabsState tabsState = new TabsState(0);
    private TabsElement tabs = tabs("[R]esumes", "[B]io", "[P]rofile", "[S]ocial").selected(0).focusable().id("nav")
            .divider(" | ")
            .title(" Navigation ").fill().state(tabsState)
            .onKeyEvent(key -> {
                if (key.code() == KeyCode.LEFT) {
                    tabsState.select(Math.max(0, tabsState.selected() - 1));
                    return EventResult.HANDLED;
                }
                if (key.code() == KeyCode.RIGHT) {
                    tabsState.select(Math.min(3, tabsState.selected() + 1));
                    return EventResult.HANDLED;
                }
                return EventResult.UNHANDLED;
            });

    private String statusMessage = "Press 'a' to add, 'x' to save, 't' to toggle theme, 'q' to exit";

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
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.loadStylesheet("catpuccin", "catpuccin.tcss");
        styleEngine.loadStylesheet("everforest", "everforest.tcss");
        styleEngine.setActiveStylesheet("catpuccin");
        try (var runner = ToolkitRunner.builder().styleEngine(styleEngine).build()) {
            runner.run(() -> {
                boolean isDirty = profileEditor.isDirty() || socialEditor.isDirty();
                String displayStatus = statusMessage;
                if (isDirty && displayStatus.startsWith("Press")) {
                    displayStatus = "*UNSAVED CHANGES* " + displayStatus;
                }

                return dock().top(tabs).center(renderContent()).bottom(panel("Status", text(displayStatus).green()))
                        .bottomHeight(Constraint.length(5)).onKeyEvent(key -> {
                            boolean isSaveKey = key.isChar('x') && !socialEditor.isDialogOpen()
                                    && !resumeSelector.isDialogOpen();
                            if (!isSaveKey) {
                                statusMessage = "Press 'a' to add, 'x' to save, 't' to toggle theme, 'q' to exit";
                            }

                            if (key.isChar('t') || key.isChar('T')) {
                                String current = styleEngine.getActiveStylesheet().orElse("catpuccin");
                                if ("catpuccin".equals(current)) {
                                    styleEngine.setActiveStylesheet("everforest");
                                } else {
                                    styleEngine.setActiveStylesheet("catpuccin");
                                }
                                return EventResult.HANDLED;
                            }
                            if (key.isChar('R') || key.isChar('r')) {
                                tabsState.select(0);
                                return EventResult.HANDLED;
                            }
                            if (key.isChar('B') || key.isChar('b')) {
                                tabsState.select(1);
                                return EventResult.HANDLED;
                            }
                            if (key.isChar('P') || key.isChar('p')) {
                                tabsState.select(2);
                                return EventResult.HANDLED;
                            }
                            if (key.isChar('S') || key.isChar('s')) {
                                tabsState.select(3);
                                return EventResult.HANDLED;
                            }

                            if (key.isChar('x') && !socialEditor.isDialogOpen()
                                    && !resumeSelector.isDialogOpen()) {
                                int tab = tabsState.selected();
                                if (tab == 3)
                                    socialEditor.save();
                                else if (tab == 2)
                                    profileEditor.save();

                                statusMessage = "Saved successfully!";
                                return EventResult.HANDLED;
                            }
                            if (key.isChar('a') && tabsState.selected() == 3
                                    && !socialEditor.isDialogOpen()) {
                                socialEditor.openAddDialog();
                                return EventResult.HANDLED;
                            }
                            if (key.isChar('e') && tabsState.selected() == 3
                                    && !socialEditor.isDialogOpen()) {
                                socialEditor.openEditDialog();
                                return EventResult.HANDLED;
                            }
                            return EventResult.UNHANDLED;

                        });
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

    private Element renderContent() {
        Integer selected = tabsState.selected();
        int tabIndex = selected != null ? selected : 0;
        Long resumeId = resumeSelector.getSelectedResumeId();

        return switch (tabIndex) {
            case 0 -> resumeSelector.render();
            case 1 -> bioEditor.render(); // TODO: Update bioEditor to accept resumeId
            case 2 -> profileEditor.render(resumeId);
            case 3 -> socialEditor.render(resumeId);
            default -> resumeSelector.render();
        };
    }

}
