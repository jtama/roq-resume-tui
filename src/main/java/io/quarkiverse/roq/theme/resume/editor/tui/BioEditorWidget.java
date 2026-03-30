package io.quarkiverse.roq.theme.resume.editor.tui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.TreeElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.widgets.form.FormState;
import dev.tamboui.widgets.tree.TreeNode;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.form;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;
import static dev.tamboui.toolkit.Toolkit.tree;

import io.quarkiverse.roq.theme.resume.editor.context.AppContext;
import io.quarkiverse.roq.theme.resume.editor.exception.YamlImportException;
import io.quarkiverse.roq.theme.resume.editor.model.Bio;
import io.quarkiverse.roq.theme.resume.editor.service.ResumeRepository;
import io.quarkiverse.roq.theme.resume.editor.service.YamlImportService;
import io.quarkiverse.roq.theme.resume.editor.util.LogoImageLoader;

@ApplicationScoped
public class BioEditorWidget {

    private final ResumeRepository repository;
    private final YamlImportService yamlImportService;
    private final ImportInputDialog importInputDialog;
    private final ImportPreviewDialog importPreviewDialog;
    private final AppContext appContext;
    private final Logger logger;

    private final TreeElement<Object> treeEl = tree().id("bioTree").focusable();
    private final FormState formState = FormState.builder()
            .textField("title", "")
            .textField("header", "")
            .textField("link", "")
            .textField("content", "")
            .textField("tags", "")
            .textField("logoLabel", "")
            .textField("logoImageUrl", "")
            .textField("logoLink", "")
            .build();
    private final LogoImageLoader logoImageLoader;

    private Long currentResumeId;
    private Bio currentBio;
    private boolean loaded = false;

    private boolean isSearching = false;
    private String searchQuery = "";
    private boolean hasModifications = false;
    private Selection selection = null;

    private String importError = null;

    /// Constructor for CDI instantiation with dependency injection.
    public BioEditorWidget(ResumeRepository repository,
            YamlImportService yamlImportService,
            ImportInputDialog importInputDialog,
            ImportPreviewDialog importPreviewDialog,
            AppContext appContext,
            LogoImageLoader logoImageLoader,
            Logger logger) {
        this.repository = repository;
        this.yamlImportService = yamlImportService;
        this.importInputDialog = importInputDialog;
        this.importPreviewDialog = importPreviewDialog;
        this.appContext = appContext;
        this.logger = logger;
        this.logoImageLoader = logoImageLoader;
    }

    public boolean isDirty() {
        return hasModifications;
    }

    public boolean isError() {
        return importError != null;
    }

    public void save() {
        if (selection != null) {
            doSave(formState);
        }
    }

    /// Trigger the import dialog. Shows a prompt to enter file path or URL.
    /// This method should be called when the user presses 'i' for import.
    public void openImportDialog() {
        if (currentResumeId == null) {
            importError = "No resume selected";
            return;
        }

        importInputDialog.open(
                this::importBio,
                this::closeImportDialog);
        importError = null;
    }

    /// Check if any import dialog is open (input or preview).
    public boolean isImportDialogOpen() {
        return importInputDialog.isOpen() || importPreviewDialog.isOpen();
    }

    /// Close the import dialog.
    public void closeImportDialog() {
        importError = null;
        importInputDialog.close();
        importPreviewDialog.close();
    }

    /// Perform the import from file or URL.
    /// @param source file path or HTTP/HTTPS URL
    public void importBio(String source) {
        if (currentResumeId == null) {
            importError = "No resume selected";
            return;
        }

        try {
            var importedBio = yamlImportService.importBio(source);

            if (importedBio.isEmpty()) {
                appContext.setStatusMessage("Failed to parse YAML");
                return;
            }

            // Show preview dialog
            importPreviewDialog.open(
                    importedBio.get(),
                    () -> confirmImport(importedBio.get()),
                    this::closeImportDialog);
        } catch (YamlImportException e) {
            importError = "Import failed: " + e.getMessage();
            appContext.setStatusMessage(importError);
            appContext.setException(e);
            logger.error("Bio Import failed ", e);
        }
    }

    /// Confirm and persist the imported Bio.
    private void confirmImport(Bio importedBio) {
        try {
            // Merge with existing bio or replace
            currentBio = importedBio;
            repository.saveBio(currentResumeId, currentBio);
            hasModifications = true;
            load(currentResumeId);
            rebuildTree();
            closeImportDialog();
            appContext.setStatusMessage("Bio imported");
            importError = null;

        } catch (Exception e) {
            importError = "Failed to save imported data: " + e.getMessage();
            appContext.setStatusMessage(importError);
            appContext.setException(e);
        }
    }

    public void load(Long resumeId) {
        this.currentResumeId = resumeId;
        this.currentBio = repository.getBio(resumeId);
        if (this.currentBio.list() == null) {
            this.currentBio = new Bio(new ArrayList<>());
        }
        this.loaded = true;
    }

    private void doSave(FormState formState) {
        if (selection == null)
            return;

        switch (selection) {
            case SelectedSection sec -> {
                var original = new Bio.Section(sec.id(), sec.title(), sec.items());
                var updated = new Bio.Section(sec.id(), formState.textValue("title"), sec.items());
                replaceInList(currentBio.list(), original, updated);
                selection = new SelectedSection(sec.id(), formState.textValue("title"), sec.items());
            }
            case SelectedItem item -> {
                List<String> tags = parseTags(formState.textValue("tags"));
                Bio.Logo logo = createLogoFromForm();

                var original = new Bio.Item(item.id(), item.header(), item.title(), item.link(), item.content(),
                        item.logo(), item.collapsible(), item.collapsed(), item.ruler(), item.tags(), item.subItems());
                var updated = new Bio.Item(item.id(), formState.textValue("header"), formState.textValue("title"),
                        formState.textValue("link"), formState.textValue("content"), logo, item.collapsible(),
                        item.collapsed(), item.ruler(), tags, item.subItems());
                replaceInTree(currentBio.list(), original, updated);
                selection = new SelectedItem(item.id(), formState.textValue("header"), formState.textValue("title"),
                        formState.textValue("link"), formState.textValue("content"), logo, item.collapsible(),
                        item.collapsed(), item.ruler(), tags, item.subItems());
            }
        }

        repository.saveBio(currentResumeId, currentBio);
        hasModifications = true;
        load(currentResumeId);
        rebuildTree();
        clearSelection();
        clearForm();
    }

    private List<String> parseTags(String tagsStr) {
        if (tagsStr == null || tagsStr.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(List.of(tagsStr.split(",")));
    }

    /// Create a Logo object from the form fields, or null if all fields are empty.
    private Bio.Logo createLogoFromForm() {
        String label = formState.textValue("logoLabel");
        String imageUrl = formState.textValue("logoImageUrl");
        String link = formState.textValue("logoLink");

        // Return null if no logo data is provided
        if ((label == null || label.isBlank()) && (imageUrl == null || imageUrl.isBlank())) {
            return null;
        }

        // Create logo with non-blank values
        return new Bio.Logo(
                label != null && !label.isBlank() ? label : null,
                imageUrl != null && !imageUrl.isBlank() ? imageUrl : null,
                link != null && !link.isBlank() ? link : null);
    }

    private void clearForm() {
        formState.setTextValue("title", "");
        formState.setTextValue("header", "");
        formState.setTextValue("link", "");
        formState.setTextValue("content", "");
        formState.setTextValue("tags", "");
        formState.setTextValue("logoLabel", "");
        formState.setTextValue("logoImageUrl", "");
        formState.setTextValue("logoLink", "");
    }

    private void populateForm() {
        if (selection == null)
            return;

        switch (selection) {
            case SelectedSection sec -> formState.setTextValue("title", sec.title() != null ? sec.title() : "");
            case SelectedItem item -> {
                clearForm();
                formState.setTextValue("title", item.title() != null ? item.title() : "");
                formState.setTextValue("header", item.header() != null ? item.header() : "");
                formState.setTextValue("link", item.link() != null ? item.link() : "");
                formState.setTextValue("content", item.content() != null ? item.content() : "");
                formState.setTextValue("tags", item.tags() != null ? String.join(",", item.tags()) : "");

                // Logo fields
                if (item.logo() != null) {
                    formState.setTextValue("logoLabel", item.logo().label() != null ? item.logo().label() : "");
                    formState.setTextValue("logoImageUrl", item.logo().imageUrl() != null ? item.logo().imageUrl() : "");
                    formState.setTextValue("logoLink", item.logo().link() != null ? item.logo().link() : "");
                } else {
                    formState.setTextValue("logoLabel", "");
                    formState.setTextValue("logoImageUrl", "");
                    formState.setTextValue("logoLink", "");
                }
            }
        }
    }

    private void clearSelection() {
        selection = null;
    }

    private void replaceInList(List<Bio.Section> list, Bio.Section old, Bio.Section updated) {
        if (list == null)
            return;
        for (int i = 0; i < list.size(); i++) {
            if (Objects.equals(list.get(i).title(), old.title())) {
                list.set(i, updated);
                return;
            }
        }
    }

    private boolean replaceInTree(List<Bio.Section> list, Bio.Item old, Bio.Item updated) {
        if (list == null)
            return false;
        for (Bio.Section sec : list) {
            if (replaceItemInList(sec.items(), old, updated))
                return true;
        }
        return false;
    }

    private boolean replaceItemInList(List<Bio.Item> list, Bio.Item old, Bio.Item updated) {
        if (list == null)
            return false;
        for (int i = 0; i < list.size(); i++) {
            if (Objects.equals(list.get(i).title(), old.title()) && Objects.equals(list.get(i).header(), old.header())) {
                list.set(i, updated);
                return true;
            }
            if (replaceItemInList(list.get(i).subItems(), old, updated))
                return true;
        }
        return false;
    }

    private void deleteSelected() {
        if (selection == null)
            return;

        switch (selection) {
            case SelectedSection sec -> {
                var toRemove = new Bio.Section(sec.id(), sec.title(), sec.items());
                currentBio.list().removeIf(s -> Objects.equals(s.title(), toRemove.title()));
            }
            case SelectedItem item -> {
                var toRemove = new Bio.Item(item.id(), item.header(), item.title(), item.link(), item.content(),
                        item.logo(), item.collapsible(), item.collapsed(), item.ruler(), item.tags(), item.subItems());
                deleteItemFromTree(currentBio.list(), toRemove);
            }
        }

        repository.saveBio(currentResumeId, currentBio);
        load(currentResumeId);
        rebuildTree();
        clearSelection();
    }

    private boolean deleteItemFromTree(List<Bio.Section> list, Bio.Item toDelete) {
        if (list == null)
            return false;
        for (Bio.Section sec : list) {
            if (sec.items() != null && sec.items().remove(toDelete))
                return true;
            if (deleteItemInList(sec.items(), toDelete))
                return true;
        }
        return false;
    }

    private boolean deleteItemInList(List<Bio.Item> list, Bio.Item toDelete) {
        if (list == null)
            return false;
        for (Bio.Item item : list) {
            if (item.subItems() != null && item.subItems().remove(toDelete))
                return true;
            if (deleteItemInList(item.subItems(), toDelete))
                return true;
        }
        return false;
    }

    private void addNewSection() {
        var newSection = new Bio.Section(null, "New Section", List.of());
        if (currentBio.list() == null) {
            currentBio = new Bio(new ArrayList<>());
        }
        currentBio.list().add(newSection);
        saveAndReload();
        selection = new SelectedSection(null, "New Section", List.of());
        populateForm();
    }

    private void addNewItem(Object parent) {
        var newItem = new Bio.Item(null, "New Header", "New Item", null, null, null, null, null, null, List.of(), List.of());

        switch (parent) {
            case Bio.Section sec -> {
                var items = sec.items() == null ? new ArrayList<Bio.Item>() : new ArrayList<>(sec.items());
                items.add(newItem);
                var updated = new Bio.Section(sec.id(), sec.title(), items);
                replaceInList(currentBio.list(), sec, updated);
            }
            case Bio.Item parentItem -> {
                var subItems = parentItem.subItems() == null ? new ArrayList<Bio.Item>()
                        : new ArrayList<>(parentItem.subItems());
                subItems.add(newItem);
                var updated = new Bio.Item(parentItem.id(), parentItem.header(), parentItem.title(),
                        parentItem.link(), parentItem.content(), parentItem.logo(), parentItem.collapsible(),
                        parentItem.collapsed(), parentItem.ruler(), parentItem.tags(), subItems);
                replaceInTree(currentBio.list(), parentItem, updated);
            }
            default -> {
            }
        }

        saveAndReload();
        selection = new SelectedItem(null, "New Header", "New Item", null, null, null, null, null, null, List.of(), List.of());
        populateForm();
    }

    private void saveAndReload() {
        repository.saveBio(currentResumeId, currentBio);
        load(currentResumeId);
        rebuildTree();
    }

    private TreeNode<Object>[] currentRoots = new TreeNode[0];
    private final java.util.Set<String> expandedKeys = new java.util.HashSet<>();

    private void collectExpandedKeys(TreeNode<Object> node) {
        if (node.isExpanded()) {
            expandedKeys.add(getNodeKey(node.data()));
        }
        if (node.children() != null) {
            for (TreeNode<Object> child : node.children()) {
                collectExpandedKeys(child);
            }
        }
    }

    private String getNodeKey(Object data) {
        return switch (data) {
            case Bio.Section sec -> "Section:" + sec.title();
            case Bio.Item item -> "Item:" + item.title() + ":" + item.header();
            default -> data != null ? data.toString() : "null";
        };
    }

    private void rebuildTree() {
        expandedKeys.clear();
        for (TreeNode<Object> root : currentRoots) {
            collectExpandedKeys(root);
        }

        List<TreeNode<Object>> roots = new ArrayList<>();
        String query = searchQuery != null ? searchQuery.toLowerCase() : "";

        if (currentBio != null && currentBio.list() != null) {
            for (Bio.Section sec : currentBio.list()) {
                String secTitle = sec.title() != null ? sec.title() : "";
                boolean matches = secTitle.toLowerCase().contains(query);
                TreeNode<Object> node = TreeNode.of(sec.title(), sec);
                boolean hasChildren = buildItemNodes(node, sec.items(), query);

                if (!isSearching || matches || hasChildren) {
                    if (isSearching && hasChildren)
                        node.expanded(true);
                    else if (expandedKeys.contains(getNodeKey(sec)))
                        node.expanded(true);
                    roots.add(node);
                }
            }
        }
        currentRoots = roots.toArray(new TreeNode[0]);
        treeEl.roots(currentRoots);
    }

    private boolean buildItemNodes(TreeNode<Object> parent, List<Bio.Item> items, String query) {
        boolean hasMatch = false;
        if (items == null)
            return false;

        for (Bio.Item item : items) {
            boolean matches = (item.title() != null && item.title().toLowerCase().contains(query))
                    || (item.header() != null && item.header().toLowerCase().contains(query))
                    || (item.tags() != null && item.tags().stream().anyMatch(t -> t.toLowerCase().contains(query)));

            TreeNode<Object> node = TreeNode.of(item.title() != null ? item.title() : "Unnamed", item);
            boolean childMatch = buildItemNodes(node, item.subItems(), query);

            if (!isSearching || matches || childMatch) {
                if (isSearching && childMatch)
                    node.expanded(true);
                else if (expandedKeys.contains(getNodeKey(item)))
                    node.expanded(true);

                if (item.subItems() == null || item.subItems().isEmpty()) {
                    node.leaf();
                }
                parent.add(node);
                hasMatch = true;
            }
        }
        return hasMatch;
    }

    private Element buildForm() {
        if (selection == null) {
            return text("Select an item to edit").fill();
        }

        return switch (selection) {
            case SelectedSection _ -> form(formState)
                    .labelWidth(10)
                    .field("title", "Title")
                    .id("Bio.Section")
                    .focusable(true)
                    .onSubmit(this::doSave);
            case SelectedItem _ -> {
                var form = form(formState)
                        .labelWidth(10)
                        .field("title", "Title")
                        .field("header", "Header")
                        .field("link", "Link")
                        .field("content", "Content")
                        .field("tags", "Tags (CSV)")
                        .group("Logo")
                        .field("logoLabel", "Label")
                        .field("logoImageUrl", "Image URL")
                        .field("logoLink", "Logo Link")
                        .endGroup()
                        .id("Bio.Item")
                        .arrowNavigation(true)
                        .focusable(true)
                        .onSubmit(this::doSave);
                yield column(form)
                        .spacing(1);
            }
        };
    }

    /// Build a logo preview element if the item has a logo.
    /// Returns null if no logo or image cannot be loaded.
    private Element buildLogoPreview(SelectedItem item) {
        if (item.logo() == null || item.logo().imageUrl() == null || item.logo().imageUrl().isBlank()) {
            return null;
        }

        var imageUrl = item.logo().imageUrl();
        var image = logoImageLoader.loadLogoImage(imageUrl, item.logo.label());
        return image.orElse(text("⚠ Logo unavailable: " + imageUrl));
    }

    private EventResult handleTreeKeyEvent(dev.tamboui.tui.event.KeyEvent key) {
        if (isSearching) {
            return handleSearchKeyEvent(key);
        }

        if (key.isCancel()) {
            if (selection != null) {
                clearSelection();
                clearForm();
                rebuildTree();
            }
            return EventResult.HANDLED;
        }

        return switch (key.code()) {
            case UP -> {
                treeEl.selectPrevious();
                yield EventResult.HANDLED;
            }
            case DOWN -> {
                treeEl.selectNext();
                yield EventResult.HANDLED;
            }
            case LEFT -> {
                var node = treeEl.selectedNode();
                if (node != null && !node.isLeaf())
                    node.expanded(false);
                yield EventResult.HANDLED;
            }
            case RIGHT -> {
                var node = treeEl.selectedNode();
                if (node != null && !node.isLeaf())
                    node.expanded(true);
                yield EventResult.HANDLED;
            }
            case ENTER -> {
                var node = treeEl.selectedNode();
                if (node != null) {
                    selection = fromNodeData(node.data());
                    populateForm();
                }
                yield EventResult.HANDLED;
            }
            case TAB -> {
                var node = treeEl.selectedNode();
                if (node != null) {
                    selection = fromNodeData(node.data());
                    populateForm();
                    yield EventResult.FOCUS_NEXT;
                }
                yield EventResult.HANDLED;
            }
            default -> {
                if (key.isChar('/')) {
                    isSearching = true;
                    searchQuery = "";
                    yield EventResult.HANDLED;
                }
                if (key.isChar('e')) {
                    var node = treeEl.selectedNode();
                    if (node != null) {
                        selection = fromNodeData(node.data());
                        populateForm();
                        yield EventResult.FOCUS_NEXT;
                    }
                }
                if (key.isChar('a')) {
                    addNewSection();
                    yield EventResult.HANDLED;
                }
                /*
                 * if (key.isChar('i')) {
                 * var node = treeEl.selectedNode();
                 * if (node != null)
                 * addNewItem(node.data());
                 * yield EventResult.HANDLED;
                 * }
                 */
                if (key.isChar('d')) {
                    deleteSelected();
                    yield EventResult.HANDLED;
                }
                if (key.isChar(' ')) {
                    treeEl.toggleSelected();
                    yield EventResult.HANDLED;
                }
                yield EventResult.UNHANDLED;
            }
        };
    }

    private EventResult handleSearchKeyEvent(dev.tamboui.tui.event.KeyEvent key) {
        if (key.code() == KeyCode.ENTER || key.isCancel()) {
            isSearching = false;
            searchQuery = "";
            rebuildTree();
            return EventResult.HANDLED;
        }
        if (key.code() == KeyCode.BACKSPACE) {
            if (!searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                rebuildTree();
            }
            return EventResult.HANDLED;
        }
        if (key.character() != 0) {
            searchQuery += key.character();
            rebuildTree();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private Selection fromNodeData(Object data) {
        return switch (data) {
            case Bio.Section sec -> new SelectedSection(sec.id(), sec.title(), sec.items());
            case Bio.Item item -> new SelectedItem(item.id(), item.header(), item.title(), item.link(), item.content(),
                    item.logo(), item.collapsible(), item.collapsed(), item.ruler(), item.tags(), item.subItems());
            default -> null;
        };
    }

    public Element render(Long resumeId) {
        if (!loaded || !Objects.equals(currentResumeId, resumeId)) {
            load(resumeId);
            rebuildTree();
        }

        // Show import input dialog first
        if (importInputDialog.isOpen()) {
            return importInputDialog.render();
        }

        // Show import preview dialog if open
        if (importPreviewDialog.isOpen()) {
            return importPreviewDialog.render();
        }

        treeEl.onKeyEvent(this::handleTreeKeyEvent);

        String footer = isSearching
                ? "SEARCH: " + searchQuery
                : (hasModifications ? "[MODIFIED] " : "")
                        + "Press '/' search, Enter select, e edit, Tab edit+focus, Esc cancel, a add section, i add/import item, d delete";

        // @formatter:off
        return panel("Bio Editor",
                row(
                        panel("Tree", treeEl).constraint(Constraint.percentage(30)),
                        panel("Details", buildForm()).fill()
                ).fill()
        ).bottomTitle(footer).fill();
        // @formatter:on
    }

    // --- Selection records ---

    private sealed interface Selection permits SelectedSection, SelectedItem {
    }

    private record SelectedSection(Long id, String title, List<Bio.Item> items) implements Selection {
    }

    private record SelectedItem(Long id, String header, String title, String link, String content,
            Bio.Logo logo, Boolean collapsible, Boolean collapsed, Boolean ruler,
            List<String> tags, List<Bio.Item> subItems) implements Selection {
    }
}