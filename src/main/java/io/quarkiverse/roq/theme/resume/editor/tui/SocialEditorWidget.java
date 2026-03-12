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
import static dev.tamboui.toolkit.Toolkit.text;

import io.quarkiverse.roq.theme.resume.editor.model.Social;
import io.quarkiverse.roq.theme.resume.editor.service.ResumeRepository;

@Singleton
public class SocialEditorWidget {

    @Inject
    ResumeRepository repository;

    private record SocialItem(String name, String url) {
    }

    private List<SocialItem> items = new ArrayList<>();
    private final TableState tableState = new TableState();
    private Long currentResumeId;

    // Dialog state
    private boolean showDialog = false;
    private boolean isEditing = false;
    private SocialItem editingItem = null;

    private final FormState dialogState = FormState.builder().textField("name", "name")
            .textField("url", "https://<URL>").build();

    public void loadResume(Long resumeId) {
        currentResumeId = resumeId;
        items.clear();
        tableState.selectFirst();
        closeDialog();
    }

    public void load() {
        if (currentResumeId == null) {
            items.clear();
            return;
        }
        Social social = repository.getSocial(currentResumeId);
        items.clear();
        if (social.items() != null) {
            for (Social.Item item : social.items()) {
                items.add(new SocialItem(item.name(), item.url()));
            }
        }
        if (!items.isEmpty()) {
            tableState.selectFirst();
        }
    }

    public void save() {
        if (currentResumeId == null) {
            return;
        }
        List<Social.Item> socialItems = items.stream().map(item -> new Social.Item(item.name(), item.url())).toList();
        repository.saveSocial(currentResumeId, new Social(socialItems));
    }

    public void openAddDialog() {
        isEditing = false;
        editingItem = null;
        dialogState.setTextValue("name", "");
        dialogState.setTextValue("url", "");
        dialogState.clearAllValidationResults();
        showDialog = true;
    }

    public void openEditDialog() {
        Integer selected = tableState.selected();
        if (selected != null && selected >= 0 && selected < items.size()) {
            isEditing = true;
            editingItem = items.get(selected);
            dialogState.setTextValue("name", editingItem.name());
            dialogState.setTextValue("url", editingItem.url());
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
            String name = dialogState.textValue("name");
            String url = dialogState.textValue("url");

            if (isEditing && editingItem != null) {
                items.set(tableState.selected(), new SocialItem(name, url));
            } else {
                items.add(new SocialItem(name, url));
                tableState.select(items.size() - 1);
            }
            save();
            closeDialog();
        }
    }

    public boolean isDialogOpen() {
        return showDialog;
    }

    public Element render() {
        if (currentResumeId == null) {
            return panel("Social Editor", text("Aucun CV charge. Selectionne un CV dans l'onglet Resumes.").yellow()).fill();
        }
        if (items.isEmpty()) {
            var saved = repository.getSocial(currentResumeId);
            if (saved.items() != null && !saved.items().isEmpty()) {
                load();
            }
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
        String title = isEditing ? "Edit Item" : "Add New Item";

        // @formatter:off
        return dialog(title,
                column(
                        formField("Name", dialogState.textField("name"))
                                .addClass("formfield")
                                .formState(dialogState, "name")
                                .id("val-name").labelWidth(10)
                                .placeholder("Acme.com")
                                .validate(Validators.required())
                                .showInlineErrors(true)
                                .focusable()
                                .onSubmit(this::submitDialog),
                        formField("Url", dialogState.textField("url"))
                                .addClass("formfield")
                                .formState(dialogState, "url")
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
                    if (key.isCancel()) {
                        closeDialog();
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                });
        // @formatter:on
    }
}
