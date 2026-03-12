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
import io.quarkiverse.roq.theme.resume.editor.tui.ResumeManagerWidget;
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
    ResumeManagerWidget resumeManager;

    private static final int TAB_COUNT = 4;

    private TabsState tabsState = new TabsState(0);
    private TabsElement tabs = tabs("[R]esumes", "[B]io", "[P]rofile", "[S]ocial")
            .selected(0)
            .focusable()
            .id("nav")
            .divider(" | ")
            .title(" Navigation ")
            .fill()
            .state(tabsState)
            .onKeyEvent(key -> switch (key.code()) {
                case KeyCode.LEFT -> {
                    tabsState.select(Math.max(0, tabsState.selected() - 1));
                    yield EventResult.HANDLED;
                }
                case KeyCode.RIGHT -> {
                    tabsState.select(Math.min(TAB_COUNT - 1, tabsState.selected() + 1));
                    yield EventResult.HANDLED;
                }
                default -> EventResult.UNHANDLED;
            });

    private String statusMessage = "Resumes: Enter load, 'n' create, '/' search, 't' toggle theme, 'q' exit";

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
            runner.run(
                    () -> dock().top(tabs).center(renderContent()).bottom(panel("Status", text(statusMessage).green()))
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
                                    selectTab(1);
                                    return EventResult.HANDLED;
                                }
                                if (key.isChar('P') || key.isChar('p')) {
                                    selectTab(2);
                                    return EventResult.HANDLED;
                                }
                                if (key.isChar('S') || key.isChar('s')) {
                                    selectTab(3);
                                    return EventResult.HANDLED;
                                }
                                if (key.isChar('R') || key.isChar('r')) {
                                    selectTab(0);
                                    return EventResult.HANDLED;
                                }
                                if (tabsState.selected() == 0 && !resumeManager.isDialogOpen() && key.isChar('n')) {
                                    resumeManager.openCreateDialog();
                                    statusMessage = defaultStatusMessage();
                                    return EventResult.HANDLED;
                                }
                                if (tabsState.selected() == 0 && !resumeManager.isDialogOpen()
                                        && (key.isChar('/') || key.character() == '/')) {
                                    resumeManager.openSearchDialog();
                                    statusMessage = defaultStatusMessage();
                                    return EventResult.HANDLED;
                                }
                                if (tabsState.selected() == 0 && !resumeManager.isDialogOpen() && key.isConfirm()) {
                                    applyResumeSelection(resumeManager.loadSelectedResume());
                                    statusMessage = defaultStatusMessage();
                                    return EventResult.HANDLED;
                                }
                                if (key.isChar('x') && !socialEditor.isDialogOpen() && !resumeManager.isDialogOpen()) {
                                    int tab = tabsState.selected();
                                    if (tab == 3)
                                        socialEditor.save();
                                    else if (tab == 1)
                                        bioEditor.save();
                                    else if (tab == 2)
                                        profileEditor.save();

                                    resumeManager.refresh();
                                    statusMessage = "Saved successfully!";
                                    return EventResult.HANDLED;
                                }
                                if (key.isChar('e') && tabsState.selected() == 1) {
                                    bioEditor.toggleEditMode();
                                    statusMessage = defaultStatusMessage();
                                    return EventResult.HANDLED;
                                }
                                if (key.isChar('a') && tabsState.selected() == 1 && bioEditor.isEditing()) {
                                    bioEditor.addItem();
                                    return EventResult.HANDLED;
                                }
                                if (key.isChar('d') && tabsState.selected() == 1 && bioEditor.isEditing()) {
                                    bioEditor.deleteSelection();
                                    return EventResult.HANDLED;
                                }
                                if (key.isChar('a') && tabsState.selected() == 3 && !socialEditor.isDialogOpen()) {
                                    socialEditor.openAddDialog();
                                    return EventResult.HANDLED;
                                }
                                if (key.isChar('e') && tabsState.selected() == 3 && !socialEditor.isDialogOpen()) {
                                    socialEditor.openEditDialog();
                                    return EventResult.HANDLED;
                                }
                                return EventResult.UNHANDLED;

                            }));
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
        applyResumeSelection(resumeManager.consumePendingResumeId());
        Integer selected = tabsState.selected();
        int tabIndex = selected != null ? selected : 0;
        return switch (tabIndex) {
            case 0 -> resumeManager.render();
            case 1 -> bioEditor.render();
            case 2 -> profileEditor.render();
            case 3 -> socialEditor.render();
            default -> resumeManager.render();
        };
    }

    private void selectTab(int index) {
        tabsState.select(index);
        statusMessage = defaultStatusMessage();
    }

    private String defaultStatusMessage() {
        return switch (tabsState.selected() != null ? tabsState.selected() : 0) {
            case 0 -> "Resumes: Enter load, 'n' create, '/' search, 't' toggle theme, 'q' exit";
            case 1 -> bioEditor.isEditing()
                    ? "Bio edit mode: 'a' add child, 'd' delete, 'x' save, 'e' leave edit mode"
                    : "Bio read mode: 'e' edit, 'x' save, 't' toggle theme, 'q' exit";
            case 3 -> "Social: 'a' add, 'e' edit, 'x' save, 't' toggle theme, 'q' exit";
            default -> "Press 'e' to edit, 'x' to save, 't' to toggle theme, 'q' to exit";
        };
    }

    private void applyResumeSelection(Long resumeId) {
        if (resumeId == null) {
            return;
        }
        bioEditor.loadResume(resumeId);
        profileEditor.loadResume(resumeId);
        socialEditor.loadResume(resumeId);
    }

}
