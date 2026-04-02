package io.quarkiverse.roq.theme.resume.editor.tui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.widgets.form.FormState;
import dev.tamboui.widgets.form.Validators;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dialog;
import static dev.tamboui.toolkit.Toolkit.formField;
import static dev.tamboui.toolkit.Toolkit.text;

import io.quarkiverse.roq.theme.resume.editor.service.YamlImportService;

/// Dialog widget for importing Bio - allows user to enter file path or URL.
/// Shows input field with validation and handles Enter (confirm) / Escape (cancel).
@ApplicationScoped
public class ImportInputDialog {

    @Inject
    YamlImportService importService;

    private static final String SOURCE_FIELD = "source";
    private boolean isOpen = false;
    private Runnable onCancel;
    private java.util.function.Consumer<String> onSubmit;

    private final FormState formState = FormState.builder()
            .textField(SOURCE_FIELD, "")
            .build();

    /// Check if the dialog is currently open.
    public boolean isOpen() {
        return isOpen;
    }

    /// Open the dialog for user to input file path or URL.
    /// @param onSubmit callback with the entered source (file path or URL)
    /// @param onCancel callback when user cancels
    public void open(java.util.function.Consumer<String> onSubmit, Runnable onCancel) {
        this.onSubmit = onSubmit;
        this.onCancel = onCancel;
        formState.setTextValue(SOURCE_FIELD, "");
        formState.clearAllValidationResults();
        isOpen = true;
    }

    /// Close the dialog.
    public void close() {
        isOpen = false;
        formState.setTextValue(SOURCE_FIELD, "");
    }

    /// Render the dialog. Returns a dialog element if open, otherwise renders an empty element.
    public Element render() {
        if (!isOpen) {
            return text(""); // Return empty element when closed
        }

        // @formatter:off
        return dialog("Import Bio from File or URL",
                column(
                        text("Enter file path (e.g., ~/bio.yaml) or URL (e.g., https://example.com/bio.yaml):"),
                        formField("Source", formState.textField(SOURCE_FIELD))
                                .addClass("formfield")
                                .formState(formState, SOURCE_FIELD)
                                .id("import-source")
                                .labelWidth(8)
                                .placeholder("~/bio.yaml or https://example.com/bio.yaml")
                                .validate(Validators.required())
                                .showInlineErrors(true)
                                .onSubmit(this::handleSubmit)
                                .focusable()
                )
                .fill()
                )
                .length(600)
                .width(900)
                .onKeyEvent(key -> {
                    if (key.code() == KeyCode.ESCAPE) {
                        handleCancel();
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                });
        // @formatter:on
    }

    /// Handle form submission.
    private void handleSubmit() {
        if (!formState.hasValidationErrors()) {
            String source = formState.textValue(SOURCE_FIELD);
            onSubmit.accept(source);
            close();
        }
    }

    /// Handle cancel action.
    private void handleCancel() {
        onCancel.run();
        close();
    }
}
