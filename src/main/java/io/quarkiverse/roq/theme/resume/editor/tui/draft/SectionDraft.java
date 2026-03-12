package io.quarkiverse.roq.theme.resume.editor.tui.draft;

import java.util.List;

/**
 * Represents an immutable snapshot of a bio section.
 *
 * @param title the title of the section
 * @param items the immutable list of items in this section
 */
public record SectionDraft(String title, List<ItemDraft> items) {
    public SectionDraft(String title, List<ItemDraft> items) {
        this.title = title;
        // Ensure the list is immutable
        this.items = List.copyOf(items);
    }
}
