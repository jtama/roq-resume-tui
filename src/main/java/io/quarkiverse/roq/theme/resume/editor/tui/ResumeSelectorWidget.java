package io.quarkiverse.roq.theme.resume.editor.tui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

import io.quarkiverse.roq.theme.resume.editor.model.Resume;
import io.quarkiverse.roq.theme.resume.editor.service.ResumeRepository;

@Singleton
public class ResumeSelectorWidget {

    @Inject
    ResumeRepository repository;

    private List<Resume> allResumes = new ArrayList<>();
    private List<Resume> filteredResumes = new ArrayList<>();
    private final TableState tableState = new TableState();

    private String searchQuery = "";
    private boolean isSearching = false;

    // Dialog state
    private boolean showDialog = false;
    private boolean isEditing = false;
    private Long editingResumeId = null;

    private final FormState dialogForm = FormState.builder().textField("name", "").build();

    private Long selectedResumeId = 1L; // Default to first one

    public void load() {
        allResumes = repository.listResumes();
        applyFilter();
    }

    private void applyFilter() {
        if (searchQuery.isEmpty()) {
            filteredResumes = new ArrayList<>(allResumes);
        } else {
            String lowerQuery = searchQuery.toLowerCase();
            filteredResumes = allResumes.stream()
                    .filter(r -> r.name().toLowerCase().contains(lowerQuery))
                    .collect(Collectors.toList());
        }
        if (!filteredResumes.isEmpty()) {
            tableState.selectFirst();
        }
    }

    public Long getSelectedResumeId() {
        return selectedResumeId;
    }

    public void setSelectedResumeId(Long id) {
        this.selectedResumeId = id;
    }

    public boolean isSearching() {
        return isSearching;
    }

    public boolean isDialogOpen() {
        return showDialog;
    }

    public Element render() {
        if (allResumes.isEmpty()) {
            load();
        }

        if (showDialog) {
            return renderDialog();
        }

        List<Row> rows = filteredResumes.stream()
                .map(r -> {
                    String prefix = r.id().equals(selectedResumeId) ? "* " : "  ";
                    return Row.from(Cell.from(prefix + r.id()), Cell.from(r.name()));
                })
                .toList();

        // @formatter:off
        TableElement table = table().state(tableState)
                .header(Row.from(Cell.from("ID"), Cell.from("Name")))
                .widths(Constraint.length(10), Constraint.fill())
                .fill()
                .rows(rows)
                .focusable()
                .id("resumeTable")
                .onKeyEvent(key -> {
                    if (isSearching) {
                        if (key.code() == KeyCode.ENTER) {
                            isSearching = false;
                            return EventResult.HANDLED;
                        }
                        if (key.isCancel()) {
                            isSearching = false;
                            searchQuery = "";
                            applyFilter();
                            return EventResult.HANDLED;
                        }
                        if (key.code() == KeyCode.BACKSPACE) {
                            if (!searchQuery.isEmpty()) {
                                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                                applyFilter();
                            }
                            return EventResult.HANDLED;
                        }
                        if (key.character() != 0) {
                            searchQuery += key.character();
                            applyFilter();
                            return EventResult.HANDLED;
                        }
                    } else {
                        if (key.isChar('/')) {
                            isSearching = true;
                            searchQuery = "";
                            return EventResult.HANDLED;
                        }
                        if (key.code() == KeyCode.ENTER) {
                            Integer selected = tableState.selected();
                            if (selected != null && selected >= 0 && selected < filteredResumes.size()) {
                                selectedResumeId = filteredResumes.get(selected).id();
                            }
                            return EventResult.HANDLED;
                        }
                        if (key.isChar('a')) {
                            isEditing = false;
                            editingResumeId = null;
                            showDialog = true;
                            dialogForm.setTextValue("name", "");
                            return EventResult.HANDLED;
                        }
                        if (key.isChar('e')) {
                            Integer selected = tableState.selected();
                            if (selected != null && selected >= 0 && selected < filteredResumes.size()) {
                                isEditing = true;
                                Resume toEdit = filteredResumes.get(selected);
                                editingResumeId = toEdit.id();
                                dialogForm.setTextValue("name", toEdit.name());
                                showDialog = true;
                            }
                            return EventResult.HANDLED;
                        }
                        if (key.isChar('d')) {
                            Integer selected = tableState.selected();
                            if (selected != null && selected >= 0 && selected < filteredResumes.size()) {
                                repository.deleteResume(filteredResumes.get(selected).id());
                                load();
                            }
                            return EventResult.HANDLED;
                        }
                    }

                    switch (key.code()) {
                        case KeyCode.DOWN -> { tableState.selectNext(rows.size()); return EventResult.HANDLED; }
                        case KeyCode.UP -> { tableState.selectPrevious(); return EventResult.HANDLED; }
                    }
                    return EventResult.UNHANDLED;
                });
        // @formatter:on

        String footer = isSearching ? "SEARCH: " + searchQuery
                : "Press '/' to search, 'Enter' to select, 'a' to add, 'e' to edit, 'd' to delete";
        return panel("Resumes", table)
                .bottomTitle(footer)
                .fill();
    }

    public void submitDialog() {
        if (!dialogForm.hasValidationErrors()) {
            String name = dialogForm.textValue("name");
            if (!name.trim().isEmpty()) {
                if (isEditing && editingResumeId != null) {
                    repository.updateResume(editingResumeId, name);
                } else {
                    Resume created = repository.createResume(name);
                    if (created != null) {
                        selectedResumeId = created.id();
                    }
                }
                showDialog = false;
                load();
            }
        }
    }

    private Element renderDialog() {
        String title = (isEditing ? "Edit Resume" : "Add New Resume") + " (Press Enter to save)";
        return dialog(title,
                column(
                        formField("Name", dialogForm.textField("name"))
                                .addClass("formfield")
                                .formState(dialogForm, "name")
                                .id("resume-name").labelWidth(10)
                                .validate(Validators.required())
                                .showInlineErrors(true)
                                .focusable()
                                .onSubmit(this::submitDialog)))
                .length(400)
                .width(600)
                .onKeyEvent(key -> {
                    if (key.isConfirm()) {
                        submitDialog();
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                });
    }
}
