package io.quarkiverse.roq.theme.resume.editor.tui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

import io.quarkiverse.roq.theme.resume.editor.model.Resume;
import io.quarkiverse.roq.theme.resume.editor.model.ResumeSummary;
import io.quarkiverse.roq.theme.resume.editor.service.ResumeRepository;

@Singleton
public class ResumeManagerWidget {

    @Inject
    ResumeRepository repository;

    private final TableState tableState = new TableState();
    private final TableState searchTableState = new TableState();
    private final FormState createState = FormState.builder().textField("title", "").build();
    private final FormState searchState = FormState.builder().textField("query", "").build();

    private List<ResumeSummary> resumes = new ArrayList<>();
    private List<ResumeSummary> filteredResumes = new ArrayList<>();
    private boolean loaded;
    private boolean createDialogOpen;
    private boolean searchDialogOpen;
    private String lastSearchQuery = "";
    private Long activeResumeId;
    private Long pendingResumeId;

    public void refresh() {
        loaded = false;
    }

    public void openCreateDialog() {
        createState.setTextValue("title", "");
        createState.clearAllValidationResults();
        createDialogOpen = true;
    }

    public void openSearchDialog() {
        searchState.setTextValue("query", "");
        searchState.clearAllValidationResults();
        lastSearchQuery = null;
        searchDialogOpen = true;
        refreshFilteredResumes();
    }

    public boolean isDialogOpen() {
        return createDialogOpen || searchDialogOpen;
    }

    public Long consumePendingResumeId() {
        Long resumeId = pendingResumeId;
        pendingResumeId = null;
        return resumeId;
    }

    public String activeResumeTitle() {
        return resumes.stream()
                .filter(summary -> Objects.equals(summary.id(), activeResumeId))
                .map(ResumeSummary::title)
                .findFirst()
                .orElse("No CV loaded");
    }

    public Element render() {
        if (!loaded) {
            resumes = repository.listResumes();
            filteredResumes = resumes;
            loaded = true;
            restoreSelection(tableState, resumes, activeResumeId);
        }

        if (createDialogOpen) {
            return renderCreateDialog();
        }
        if (searchDialogOpen) {
            refreshFilteredResumes();
            return renderSearchDialog();
        }
        return renderList();
    }

    public Long loadSelectedResume() {
        ResumeSummary selected = selectedResume(resumes, tableState);
        if (selected == null) {
            return null;
        }
        activeResumeId = selected.id();
        pendingResumeId = selected.id();
        return selected.id();
    }

    public Long loadSelectedSearchResult() {
        ResumeSummary selected = selectedResume(filteredResumes, searchTableState);
        if (selected == null) {
            return null;
        }
        activeResumeId = selected.id();
        pendingResumeId = selected.id();
        restoreSelection(tableState, resumes, selected.id());
        closeDialogs();
        return selected.id();
    }

    private Element renderList() {
        List<Row> rows = resumes.stream().map(this::toRow).toList();
        return panel("Resumes",
                column(
                        text("Select a CV here before using the other tabs.").fill(),
                        buildTable("resumes-table", tableState, rows).fill())
                        .spacing(1).fill())
                .fill()
                .bottomTitle("Enter load | n create | / search");
    }

    private Element renderCreateDialog() {
        return dialog("Create CV",
                column(
                        formField("Title", createState.textField("title"))
                                .addClass("formfield")
                                .formState(createState, "title")
                                .id("resume-create-title")
                                .labelWidth(10)
                                .validate(Validators.required())
                                .showInlineErrors(true)
                                .focusable()
                                .onSubmit(this::createResume))
                        .spacing(1))
                .length(180)
                .width(520)
                .onKeyEvent(key -> {
                    if (key.isConfirm()) {
                        createResume();
                        return EventResult.HANDLED;
                    }
                    if (key.isCancel()) {
                        closeDialogs();
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                });
    }

    private Element renderSearchDialog() {
        List<Row> rows = filteredResumes.stream().map(this::toRow).toList();
        return dialog("Search CV (/)",
                column(
                        formField("Query", searchState.textField("query"))
                                .addClass("formfield")
                                .formState(searchState, "query")
                                .id("resume-search-query")
                                .labelWidth(10)
                                .focusable(),
                        buildTable("resumes-search-table", searchTableState, rows).fill())
                        .spacing(1))
                .length(420)
                .width(900)
                .onKeyEvent(key -> {
                    if (key.isConfirm()) {
                        loadSelectedSearchResult();
                        return EventResult.HANDLED;
                    }
                    if (key.isCancel()) {
                        closeDialogs();
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                });
    }

    private TableElement buildTable(String id, TableState state, List<Row> rows) {
        return table().state(state)
                .header(Row.from(Cell.from("CV"), Cell.from("Name"), Cell.from("Role"), Cell.from("Slug")))
                .widths(Constraint.length(28), Constraint.length(24), Constraint.length(28), Constraint.fill())
                .rows(rows)
                .id(id)
                .fill()
                .focusable()
                .onKeyEvent(key -> switch (key.code()) {
                    case KeyCode.DOWN -> {
                        state.selectNext(rows.size());
                        yield EventResult.HANDLED;
                    }
                    case KeyCode.PAGE_DOWN -> {
                        state.selectLast(rows.size());
                        yield EventResult.HANDLED;
                    }
                    case KeyCode.UP -> {
                        state.selectPrevious();
                        yield EventResult.HANDLED;
                    }
                    case KeyCode.PAGE_UP -> {
                        state.selectFirst();
                        yield EventResult.HANDLED;
                    }
                    default -> EventResult.UNHANDLED;
                });
    }

    private Row toRow(ResumeSummary summary) {
        return Row.from(
                Cell.from(summary.title()),
                Cell.from(orDash(summary.fullName())),
                Cell.from(orDash(summary.jobTitle())),
                Cell.from(summary.slug()));
    }

    private ResumeSummary selectedResume(List<ResumeSummary> values, TableState state) {
        Integer selected = state.selected();
        if (selected == null || selected < 0 || selected >= values.size()) {
            return null;
        }
        return values.get(selected);
    }

    private void refreshFilteredResumes() {
        String query = searchState.textValue("query");
        if (Objects.equals(lastSearchQuery, query)) {
            return;
        }
        filteredResumes = repository.searchResumes(query);
        restoreSelection(searchTableState, filteredResumes, activeResumeId);
        lastSearchQuery = query;
    }

    private void createResume() {
        if (createState.hasValidationErrors()) {
            return;
        }
        Resume resume = repository.createResume(createState.textValue("title"));
        loaded = false;
        resumes = repository.listResumes();
        filteredResumes = resumes;
        loaded = true;
        activeResumeId = resume.id();
        pendingResumeId = resume.id();
        restoreSelection(tableState, resumes, activeResumeId);
        closeDialogs();
    }

    private void closeDialogs() {
        createDialogOpen = false;
        searchDialogOpen = false;
    }

    private void restoreSelection(TableState state, List<ResumeSummary> values, Long selectedResumeId) {
        if (values.isEmpty()) {
            state.select(0);
            return;
        }
        if (selectedResumeId == null) {
            state.selectFirst();
            return;
        }
        for (int i = 0; i < values.size(); i++) {
            if (Objects.equals(values.get(i).id(), selectedResumeId)) {
                state.select(i);
                return;
            }
        }
        state.selectFirst();
    }

    private String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
