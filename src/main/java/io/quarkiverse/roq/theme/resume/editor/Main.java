package io.quarkiverse.roq.theme.resume.editor;

import java.util.Scanner;

import jakarta.inject.Inject;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;

import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.tabs;

import io.quarkiverse.roq.theme.resume.editor.context.AppContext;
import io.quarkiverse.roq.theme.resume.editor.context.GlobalKeyHandler;
import io.quarkiverse.roq.theme.resume.editor.context.ThemeManager;
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

    @Inject
    io.quarkiverse.roq.theme.resume.editor.tui.HelpWidget helpWidget;

    @Inject
    AppContext appContext;

    @Inject
    ThemeManager themeManager;

    @Inject
    GlobalKeyHandler globalKeyHandler;

    @Override
    public int run(String... args) throws Exception {
        initPassword();

        StyleEngine styleEngine = StyleEngine.create();
        themeManager.initialize(styleEngine);

        var tabs = tabs("[R]esumes", "[B]io", "[P]rofile", "[S]ocial", "[H]elp")
                .selected(0).focusable().id("nav")
                .divider(" | ")
                .title(" Navigation ").fill()
                .state(appContext.tabsState());

        try (var runner = ToolkitRunner.builder().styleEngine(styleEngine).build()) {
            runner.run(() -> dock()
                    .top(tabs)
                    .center(renderContent())
                    .bottom(panel("Status", appContext.displayStatus()))
                    .bottomHeight(Constraint.length(5))
                    .onKeyEvent(globalKeyHandler::handle));
        }

        shutdown();
        return 0;
    }

    private void initPassword() {
        if (!cryptoService.isPasswordSet()) {
            System.out.print("Enter database password: ");
            if (System.console() != null) {
                char[] pass = System.console().readPassword();
                try {
                    cryptoService.initialize(new String(pass));
                } catch (Exception e) {
                    System.err.println("Failed to initialize: " + e.getMessage());
                }
            } else {
                Scanner scanner = new Scanner(System.in);
                if (scanner.hasNextLine()) {
                    try {
                        cryptoService.initialize(scanner.nextLine());
                    } catch (Exception e) {
                        System.err.println("Failed to initialize: " + e.getMessage());
                    }
                } else {
                    System.err.println("No password provided.");
                }
            }
        } else {
            try {
                cryptoService.initialize(null);
            } catch (Exception e) {
                System.err.println("Failed to initialize: " + e.getMessage());
            }
        }
    }

    private void shutdown() {
        try {
            cryptoService.saveAndEncrypt();
            System.out.println("Database saved and encrypted.");
        } catch (Exception e) {
            System.err.println("Failed to save database: " + e.getMessage());
        }
    }

    private Element renderContent() {
        Integer selected = appContext.tabsState().selected();
        int tabIndex = selected != null ? selected : 0;
        Long resumeId = resumeSelector.getSelectedResumeId();

        return switch (tabIndex) {
            case 0 -> resumeSelector.render();
            case 1 -> bioEditor.render(resumeId);
            case 2 -> profileEditor.render(resumeId);
            case 3 -> socialEditor.render(resumeId);
            case 4 -> helpWidget.render();
            default -> resumeSelector.render();
        };
    }
}