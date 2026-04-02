package io.quarkiverse.roq.theme.resume.editor.tui;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.TableElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.widgets.form.FormState;
import dev.tamboui.widgets.form.Validators;
import dev.tamboui.widgets.table.Cell;
import dev.tamboui.widgets.table.Row;
import dev.tamboui.widgets.table.TableState;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dialog;
import static dev.tamboui.toolkit.Toolkit.formField;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.table;

import io.quarkiverse.roq.theme.resume.editor.model.Social;
import io.quarkiverse.roq.theme.resume.editor.service.ResumeRepository;

@Singleton
public class SocialEditorWidget {

    public static final String NAME_FORM_STATE = "name";
    public static final String URL_FORM_STATE = "url";
    @Inject
    ResumeRepository repository;

    private List<Social.Item> items = new ArrayList<>();
    private List<Social.Item> originalItems = new ArrayList<>();
    private final TableState tableState = new TableState();

    // Dialog state
    private boolean showDialog = false;
    private boolean isEditing = false;
    private Social.Item editingItem = null;

    private final FormState dialogState = FormState.builder().textField(NAME_FORM_STATE, "")
            .textField(URL_FORM_STATE, "https://").build();

    private Long currentResumeId;

    public void load(Long resumeId) {
        this.currentResumeId = resumeId;
        Social social = repository.getSocial(resumeId);
        items.clear();
        if (social.items() != null) {
            items.addAll(social.items());
        }
        originalItems.clear();
        originalItems.addAll(items);
        if (!items.isEmpty()) {
            tableState.selectFirst();
        }
    }

    public boolean isDirty() {
        return !items.equals(originalItems);
    }

    public void save() {
        if (currentResumeId == null)
            return;

        repository.saveSocial(currentResumeId, new Social(new ArrayList<>(items)));
        originalItems.clear();
        originalItems.addAll(items);
    }

    public void openAddDialog() {
        isEditing = false;
        editingItem = null;
        dialogState.setTextValue(NAME_FORM_STATE, "");
        dialogState.setTextValue(URL_FORM_STATE, "");
        dialogState.clearAllValidationResults();
        showDialog = true;
    }

    public void openEditDialog() {
        Integer selected = tableState.selected();
        if (selected != null && selected >= 0 && selected < items.size()) {
            isEditing = true;
            editingItem = items.get(selected);
            dialogState.setTextValue(NAME_FORM_STATE, editingItem.name());
            dialogState.setTextValue(URL_FORM_STATE, editingItem.url());
            dialogState.clearAllValidationResults();
            showDialog = true;
        }
    }

    public void closeDialog() {
        showDialog = false;
        editingItem = null;
    }

    public void submitDialog() {
        if (!dialogState.hasValidationErrors()) {
            String name = dialogState.textValue(NAME_FORM_STATE);
            String url = dialogState.textValue(URL_FORM_STATE);

            if (isEditing && editingItem != null) {
                items.set(tableState.selected(), new Social.Item(name, url));
            } else {
                items.add(new Social.Item(name, url));
                tableState.select(items.size() - 1);
            }
            save();
            closeDialog();
        }
    }

    public boolean isDialogOpen() {
        return showDialog;
    }

    public Element render(Long resumeId) {
        if (items.isEmpty() || !resumeId.equals(currentResumeId)) {
            load(resumeId);
        }

        if (showDialog) {
            return renderDialog();
        }

        return renderTable();
    }

    private Element renderTable() {
        List<Row> rows = items.stream().map(item -> Row.from(Cell.from(item.name()), Cell.from(item.url()))).toList();
        // @formatter:off
        TableElement table = table().state(tableState)
                .header(
                        Row.from(Cell.from("Type"), Cell.from("Name"), Cell.from("URL")))
                .widths(
                        Constraint.length(70), Constraint.fill())
                .fill()
                .rows(rows)
                .focusable()
                .id("socialTable")
                .onKeyEvent(key -> switch (key.code()){
                    case KeyCode.DOWN -> {
                        tableState.selectNext(rows.size());
                        yield EventResult.HANDLED;
                    }
                    case KeyCode.PAGE_DOWN -> {
                        tableState.selectLast(rows.size());
                        yield EventResult.HANDLED;
                    }
                    case KeyCode.UP -> {
                        tableState.selectPrevious();
                        yield EventResult.HANDLED;
                    }
                    case KeyCode.PAGE_UP -> {
                        tableState.selectFirst();
                        yield EventResult.HANDLED;
                    }
                    default -> EventResult.UNHANDLED;
                });
        // @formatter:on

        return panel("Social Editor", table).fill();
    }

    private Element renderDialog() {
        String title = (isEditing ? "Edit Item" : "Add New Item") + " (Press Enter to save)";

    // @formatter:off
        return dialog(title,
                column(
                        formField("Name", dialogState.textField(NAME_FORM_STATE))
                                .addClass("formfield")
                                .formState(dialogState, NAME_FORM_STATE)
                                .id("val-name").labelWidth(10)
                                .placeholder("Acme.com")
                                .validate(Validators.required())
                                .showInlineErrors(true)
                                .focusable()
                                .onSubmit(this::submitDialog),
                        formField("Url", dialogState.textField(URL_FORM_STATE))
                                .addClass("formfield")
                                .formState(dialogState, URL_FORM_STATE)
                                .id("val-url").labelWidth(10)
                                .placeholder("https://acme.com/profile_id")
                                .validate(Validators.required(),
                                        Validators.pattern(
                                                "^https?:\\/\\/(?:www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&\\/=]*)$",
                                                "Format: https://acme.com/"))
                                .showInlineErrors(true)
                                .focusable()
                                .onSubmit(this::submitDialog)
                )
                        )
                .length(400)
                .width(700)
                .onKeyEvent(key -> {
                    if (key.isConfirm()) {
                        submitDialog();
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                });
        // @formatter:on
    }

    public boolean isError() {
        return false;
    }
}
