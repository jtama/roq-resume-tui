package io.quarkiverse.roq.theme.resume.editor.tui;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.widgets.form.FormState;
import dev.tamboui.widgets.form.Validators;
import dev.tamboui.widgets.table.Cell;
import dev.tamboui.widgets.table.Row;
import dev.tamboui.widgets.table.TableState;

import static dev.tamboui.toolkit.Toolkit.*;

import io.quarkiverse.roq.theme.resume.editor.model.ResumeSummary;
import io.quarkiverse.roq.theme.resume.editor.service.CVService;

@Singleton
public class CVManagerWidget {

    @Inject
    CVService cvService;

    private List<ResumeSummary> resumes;
    private final TableState tableState = new TableState();
    private boolean showAddDialog = false;
    private final FormState addDialogState = FormState.builder().textField("title", "").textField("slug", "").build();

    public void load() {
        resumes = cvService.getResumes();
        if (resumes != null && !resumes.isEmpty()) {
            tableState.selectFirst();
        }
    }

    public Element render() {
        if (resumes == null) {
            load();
        }

        if (showAddDialog) {
            return renderAddDialog();
        }

        return panel("CV Manager", renderTable()).fill();
    }

    private Element renderTable() {
        List<Row> rows = resumes.stream()
                .map(r -> Row.from(Cell.from(r.title()), Cell.from(r.slug())))
                .collect(Collectors.toList());

        return table().state(tableState)
                .header(Row.from(Cell.from("Title"), Cell.from("Slug")))
                .widths(Constraint.fill(), Constraint.length(20))
                .rows(rows)
                .fill()
                .focusable()
                .id("cvTable")
                .onKeyEvent(key -> {
                    if (key.isChar('a')) {
                        openAddDialog();
                        return EventResult.HANDLED;
                    }
                    if (key.code() == KeyCode.DOWN) {
                        tableState.selectNext(rows.size());
                        return EventResult.HANDLED;
                    }
                    if (key.code() == KeyCode.UP) {
                        tableState.selectPrevious();
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                });
    }

    public Optional<ResumeSummary> getSelectedResume() {
        if (tableState.selected() != null && tableState.selected() < resumes.size()) {
            return Optional.of(resumes.get(tableState.selected()));
        }
        return Optional.empty();
    }

    private Element renderAddDialog() {
        return dialog("Add New CV",
                column(
                        formField("Title", addDialogState.textField("title"))
                                .validate(Validators.required())
                                .showInlineErrors(true)
                                .focusable(),
                        formField("Slug", addDialogState.textField("slug"))
                                .validate(Validators.required(),
                                        Validators.pattern("^[a-z0-9-]+$",
                                                "Slug can only contain lowercase letters, numbers and hyphens"))
                                .showInlineErrors(true)
                                .focusable()))
                .onKeyEvent(key -> {
                    if (key.isConfirm()) {
                        submitAddDialog();
                        return EventResult.HANDLED;
                    }
                    if (key.isCancel()) {
                        closeAddDialog();
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                });
    }

    private void openAddDialog() {
        addDialogState.setTextValue("title", "");
        addDialogState.setTextValue("slug", "");
        addDialogState.clearAllValidationResults();
        showAddDialog = true;
    }

    private void closeAddDialog() {
        showAddDialog = false;
    }

    private void submitAddDialog() {
        if (!addDialogState.hasValidationErrors()) {
            String title = addDialogState.textValue("title");
            String slug = addDialogState.textValue("slug");
            cvService.create(title, slug);
            load();
            closeAddDialog();
        }
    }

}
