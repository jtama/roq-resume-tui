package io.quarkiverse.roq.theme.resume.editor.tui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.TreeElement;
import dev.tamboui.widgets.form.FieldType;
import dev.tamboui.widgets.form.FormState;
import dev.tamboui.widgets.form.Validators;
import dev.tamboui.widgets.tree.TreeNode;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.formField;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.percent;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;
import static dev.tamboui.toolkit.Toolkit.tree;

import io.quarkiverse.roq.theme.resume.editor.model.Bio;
import io.quarkiverse.roq.theme.resume.editor.service.ResumeRepository;
import io.quarkiverse.roq.theme.resume.editor.tui.draft.ItemDraft;
import io.quarkiverse.roq.theme.resume.editor.tui.draft.SectionDraft;
import io.quarkiverse.roq.theme.resume.editor.tui.mapper.BioEditorMapper;

@Singleton
public class BioEditorWidget {

    @Inject
    ResumeRepository repository;

    @Inject
    BioEditorMapper mapper;

    private boolean loaded = false;
    private boolean editing = false;
    private List<SectionDraft> sections = new ArrayList<>();
    private Long currentResumeId;

    private final FormState form = FormState.builder()
            .textField("sectionTitle", "")
            .textField("itemHeader", "")
            .textField("itemTitle", "")
            .textField("itemLink", "")
            .textField("itemContent", "")
            .textField("logoLabel", "")
            .textField("logoImageUrl", "")
            .textField("logoLink", "")
            .booleanField("collapsible", false)
            .booleanField("collapsed", false)
            .booleanField("ruler", false)
            .build();

    private TreeElement<SelectionRef> readTree;
    private TreeElement<SelectionRef> editTree;
    private SelectionRef activeSelection;

    public void loadResume(Long resumeId) {
        currentResumeId = resumeId;
        loaded = false;
        editing = false;
        sections = new ArrayList<>();
        activeSelection = null;
        clearForm();
        refreshTrees();
    }

    public void load() {
        if (currentResumeId == null) {
            sections = new ArrayList<>();
            loaded = true;
            activeSelection = null;
            refreshTrees();
            return;
        }
        Bio bio = repository.getBio(currentResumeId);
        sections = bio.list() != null ? bio.list().stream().map(mapper::toDraft).toList() : new ArrayList<>();
        loaded = true;
        if (sections.isEmpty()) {
            activeSelection = null;
        } else {
            select(new SectionSelection(sections.getFirst()));
        }
        refreshTrees();
    }

    public void save() {
        if (currentResumeId == null) {
            return;
        }
        applyFormToSelection();
        repository.saveBio(currentResumeId, new Bio(sections.stream().map(mapper::toSection).toList()));
    }

    public boolean isEditing() {
        return editing;
    }

    public void toggleEditMode() {
        if (!loaded) {
            load();
        }
        if (editing) {
            applyFormToSelection();
        } else if (sections.isEmpty()) {
            addSection();
        }
        editing = !editing;
        refreshTrees();
    }

    public void addSection() {
        applyFormToSelection();
        var mutableSections = new ArrayList<>(sections);
        var newSection = new SectionDraft("New section", Collections.emptyList());
        mutableSections.add(newSection);
        sections = List.copyOf(mutableSections);
        select(new SectionSelection(newSection));
        refreshTrees();
    }

    public void addItem() {
        if (!editing) {
            return;
        }
        applyFormToSelection();
        ItemDraft newItem = new ItemDraft(null, "New item", null, null, null, null, null, false, false, false,
                Collections.emptyList());

        if (activeSelection instanceof SectionSelection sel) {
            var newItems = new ArrayList<>(sel.section().items());
            newItems.add(newItem);
            var newSection = new SectionDraft(sel.section().title(), newItems);
            updateSection(sel.section(), newSection);
            select(new ItemSelection(newItem));
        } else if (activeSelection instanceof ItemSelection sel) {
            var newSubItems = new ArrayList<>(sel.item().subItems());
            newSubItems.add(newItem);
            var updatedItem = new ItemDraft(sel.item().header(), sel.item().title(), sel.item().link(), sel.item().content(),
                    sel.item().logoLabel(), sel.item().logoImageUrl(), sel.item().logoLink(), sel.item().collapsible(),
                    sel.item().collapsed(), sel.item().ruler(), newSubItems);
            updateItem(sel.item(), updatedItem);
            select(new ItemSelection(newItem));
        }
        refreshTrees();
    }

    public void deleteSelection() {
        if (!editing || activeSelection == null) {
            return;
        }

        if (activeSelection instanceof SectionSelection sel) {
            sections = sections.stream().filter(s -> s != sel.section()).toList();
        } else if (activeSelection instanceof ItemSelection sel) {
            sections = sections.stream().map(section -> deleteItemRecursive(section, sel.item())).toList();
        }

        if (sections.isEmpty()) {
            activeSelection = null;
            clearForm();
        } else {
            select(new SectionSelection(sections.getFirst()));
        }
        refreshTrees();
    }

    private SectionDraft deleteItemRecursive(SectionDraft section, ItemDraft itemToDelete) {
        List<ItemDraft> newItems = section.items().stream()
                .filter(item -> item != itemToDelete)
                .map(item -> deleteSubItemRecursive(item, itemToDelete))
                .toList();
        return new SectionDraft(section.title(), newItems);
    }

    private ItemDraft deleteSubItemRecursive(ItemDraft currentItem, ItemDraft itemToDelete) {
        List<ItemDraft> newSubItems = currentItem.subItems().stream()
                .filter(item -> item != itemToDelete)
                .map(subItem -> deleteSubItemRecursive(subItem, itemToDelete))
                .toList();
        return new ItemDraft(currentItem.header(), currentItem.title(), currentItem.link(), currentItem.content(),
                currentItem.logoLabel(), currentItem.logoImageUrl(), currentItem.logoLink(), currentItem.collapsible(),
                currentItem.collapsed(), currentItem.ruler(), newSubItems);
    }

    private void updateSection(SectionDraft oldSection, SectionDraft newSection) {
        sections = sections.stream()
                .map(s -> s == oldSection ? newSection : s)
                .toList();
    }

    private void updateItem(ItemDraft oldItem, ItemDraft newItem) {
        sections = sections.stream()
                .map(section -> updateItemRecursive(section, oldItem, newItem))
                .toList();
    }

    private SectionDraft updateItemRecursive(SectionDraft section, ItemDraft oldItem, ItemDraft newItem) {
        List<ItemDraft> updatedItems = section.items().stream()
                .map(item -> item == oldItem ? newItem : updateSubItemRecursive(item, oldItem, newItem))
                .toList();
        return new SectionDraft(section.title(), updatedItems);
    }

    private ItemDraft updateSubItemRecursive(ItemDraft current, ItemDraft oldItem, ItemDraft newItem) {
        if (current == oldItem) {
            return newItem;
        }
        List<ItemDraft> updatedSubItems = current.subItems().stream()
                .map(subItem -> subItem == oldItem ? newItem : updateSubItemRecursive(subItem, oldItem, newItem))
                .toList();
        return new ItemDraft(current.header(), current.title(), current.link(), current.content(),
                current.logoLabel(), current.logoImageUrl(), current.logoLink(), current.collapsible(),
                current.collapsed(), current.ruler(), updatedSubItems);
    }

    public Element render() {
        if (!loaded) {
            load();
        }
        if (currentResumeId == null) {
            return panel("Bio", text("Aucun CV charge. Selectionne un CV dans l'onglet Resumes.").yellow()).fill();
        }
        refreshSelectionFromTree();
        return editing ? renderEditMode() : renderReadMode();
    }

    private Element renderReadMode() {
        return panel("Bio", ensureReadTree().fill()).fill().bottomTitle("Read mode. Press 'e' to edit.");
    }

    private Element renderEditMode() {
        return panel("Bio Editor",
                row(panel("Structure", ensureEditTree().fill()).constraint(percent(45)).fill(),
                        panel("Details", renderSelectionForm()).constraint(percent(55)).fill()).spacing(1).fill())
                .fill()
                .bottomTitle("Edit mode. Press 'a' to add, 'd' to delete, 'x' to save, 'e' to leave edit mode.");
    }

    private Element renderSelectionForm() {
        if (activeSelection == null) {
            return text("No section yet. Press 'a' to create one.").yellow().fill();
        }

        if (activeSelection instanceof SectionSelection) {
            return column(formField("Section Title", form.textField("sectionTitle")).addClass("formfield")
                    .formState(form, "sectionTitle").id("bio-section-title").labelWidth(14)
                    .validate(Validators.required()).showInlineErrors(true).focusable()).spacing(1).fill();
        }

        return column(
                row(formField("Header", form.textField("itemHeader")).addClass("formfield").formState(form, "itemHeader")
                        .id("bio-item-header").labelWidth(12).fill().focusable(),
                        formField("Title", form.textField("itemTitle")).addClass("formfield")
                                .formState(form, "itemTitle").id("bio-item-title").labelWidth(12).fill()
                                .validate(Validators.required()).showInlineErrors(true).focusable())
                        .spacing(1),
                formField("Link", form.textField("itemLink")).addClass("formfield").formState(form, "itemLink")
                        .id("bio-item-link").labelWidth(12).fill().focusable(),
                formField("Content", form.textField("itemContent")).addClass("formfield")
                        .formState(form, "itemContent").id("bio-item-content").labelWidth(12).fill().focusable(),
                row(formField("Logo Label", form.textField("logoLabel")).addClass("formfield")
                        .formState(form, "logoLabel").id("bio-logo-label").labelWidth(12).fill().focusable(),
                        formField("Logo Url", form.textField("logoImageUrl")).addClass("formfield")
                                .formState(form, "logoImageUrl").id("bio-logo-url").labelWidth(12).fill().focusable())
                        .spacing(1),
                formField("Logo Link", form.textField("logoLink")).addClass("formfield").formState(form, "logoLink")
                        .id("bio-logo-link").labelWidth(12).fill().focusable(),
                row(formField("Collapsible", form.booleanField("collapsible"), FieldType.CHECKBOX)
                        .formState(form, "collapsible").id("bio-collapsible").focusable(),
                        formField("Collapsed", form.booleanField("collapsed"), FieldType.CHECKBOX)
                                .formState(form, "collapsed").id("bio-collapsed").focusable(),
                        formField("Ruler", form.booleanField("ruler"), FieldType.CHECKBOX).formState(form, "ruler")
                                .id("bio-ruler").focusable())
                        .spacing(2))
                .spacing(1).fill();
    }

    private TreeElement<SelectionRef> ensureReadTree() {
        if (readTree == null) {
            readTree = buildTree("bio-read-tree");
        }
        return readTree;
    }

    private TreeElement<SelectionRef> ensureEditTree() {
        if (editTree == null) {
            editTree = buildTree("bio-edit-tree");
        }
        return editTree;
    }

    private TreeElement<SelectionRef> buildTree(String id) {
        TreeNode<SelectionRef>[] roots = sections.stream().map(this::toTreeNode).toArray(TreeNode[]::new);
        return tree(roots).id(id).selected(selectedIndex(roots)).title(" Structure ")
                .scrollbar(TreeElement.ScrollBarPolicy.AS_NEEDED).highlightSymbol("> ").focusable();
    }

    private TreeNode<SelectionRef> toTreeNode(SectionDraft section) {
        var node = TreeNode.<SelectionRef> of(label(section), new SectionSelection(section)).expanded();
        section.items().forEach(item -> node.add(toTreeNode(item)));
        return node;
    }

    private TreeNode<SelectionRef> toTreeNode(ItemDraft item) {
        var node = TreeNode.<SelectionRef> of(label(item), new ItemSelection(item)).expanded();
        if (item.subItems().isEmpty()) {
            node.leaf();
        } else {
            item.subItems().forEach(child -> node.add(toTreeNode(child)));
        }
        return node;
    }

    private void refreshTrees() {
        readTree = null;
        editTree = null;
    }

    private void refreshSelectionFromTree() {
        TreeElement<SelectionRef> treeElement = editing ? editTree : readTree;
        if (treeElement == null)
            return;
        var selectedNode = treeElement.selectedNode();
        if (selectedNode != null && selectedNode.data() != null && !selectedNode.data().equals(activeSelection)) {
            select(selectedNode.data());
        }
    }

    private int selectedIndex(TreeNode<SelectionRef>[] roots) {
        if (activeSelection == null)
            return 0;
        List<SelectionRef> flattened = new ArrayList<>();
        for (TreeNode<SelectionRef> root : roots) {
            flatten(root, flattened);
        }
        int index = IntStream.range(0, flattened.size())
                .filter(i -> flattened.get(i).equals(activeSelection))
                .findFirst()
                .orElse(-1);
        return Math.max(index, 0);
    }

    private void flatten(TreeNode<SelectionRef> node, List<SelectionRef> flattened) {
        flattened.add(node.data());
        node.children().forEach(child -> flatten(child, flattened));
    }

    private void select(SelectionRef selection) {
        activeSelection = selection;
        syncFormFromSelection();
    }

    private void syncFormFromSelection() {
        clearForm();
        if (activeSelection instanceof SectionSelection sel) {
            form.setTextValue("sectionTitle", value(sel.section().title()));
        } else if (activeSelection instanceof ItemSelection sel) {
            ItemDraft item = sel.item();
            form.setTextValue("itemHeader", value(item.header()));
            form.setTextValue("itemTitle", value(item.title()));
            form.setTextValue("itemLink", value(item.link()));
            form.setTextValue("itemContent", value(item.content()));
            form.setTextValue("logoLabel", value(item.logoLabel()));
            form.setTextValue("logoImageUrl", value(item.logoImageUrl()));
            form.setTextValue("logoLink", value(item.logoLink()));
            form.setBooleanValue("collapsible", item.collapsible());
            form.setBooleanValue("collapsed", item.collapsed());
            form.setBooleanValue("ruler", item.ruler());
        }
    }

    private void applyFormToSelection() {
        if (activeSelection == null) {
            return;
        }

        if (activeSelection instanceof SectionSelection sel) {
            var newSection = new SectionDraft(trimToDefault(form.textValue("sectionTitle"), "Untitled section"),
                    sel.section().items());
            updateSection(sel.section(), newSection);
            activeSelection = new SectionSelection(newSection);
        } else if (activeSelection instanceof ItemSelection sel) {
            var newItem = new ItemDraft(
                    trimToNull(form.textValue("itemHeader")),
                    trimToDefault(form.textValue("itemTitle"), "Untitled item"),
                    trimToNull(form.textValue("itemLink")),
                    trimToNull(form.textValue("itemContent")),
                    trimToNull(form.textValue("logoLabel")),
                    trimToNull(form.textValue("logoImageUrl")),
                    trimToNull(form.textValue("logoLink")),
                    form.booleanValue("collapsible"),
                    form.booleanValue("collapsed"),
                    form.booleanValue("ruler"),
                    sel.item().subItems());
            updateItem(sel.item(), newItem);
            activeSelection = new ItemSelection(newItem);
        }
    }

    private void clearForm() {
        form.setTextValue("sectionTitle", "");
        form.setTextValue("itemHeader", "");
        form.setTextValue("itemTitle", "");
        form.setTextValue("itemLink", "");
        form.setTextValue("itemContent", "");
        form.setTextValue("logoLabel", "");
        form.setTextValue("logoImageUrl", "");
        form.setTextValue("logoLink", "");
        form.setBooleanValue("collapsible", false);
        form.setBooleanValue("collapsed", false);
        form.setBooleanValue("ruler", false);
        form.clearAllValidationResults();
    }

    private String label(SectionDraft section) {
        return value(section.title()).isBlank() ? "Untitled section" : section.title();
    }

    private String label(ItemDraft item) {
        String header = trimToNull(item.header());
        String title = trimToNull(item.title());
        if (header != null && title != null) {
            return header + " | " + title;
        }
        if (title != null) {
            return title;
        }
        return Objects.requireNonNullElse(header, "Untitled item");
    }

    private String value(String value) {
        return value != null ? value : "";
    }

    private sealed interface SelectionRef permits SectionSelection, ItemSelection {
    }

    private record SectionSelection(SectionDraft section) implements SelectionRef {
    }

    private record ItemSelection(ItemDraft item) implements SelectionRef {
    }

    private String trimToNull(String value) {
        if (value == null)
            return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed != null ? trimmed : defaultValue;
    }
}
