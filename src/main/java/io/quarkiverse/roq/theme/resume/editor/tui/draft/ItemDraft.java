package io.quarkiverse.roq.theme.resume.editor.tui.draft;

import java.util.List;

/**
 * Represents an immutable snapshot of a bio item.
 */
public record ItemDraft(
        String header,
        String title,
        String link,
        String content,
        String logoLabel,
        String logoImageUrl,
        String logoLink,
        boolean collapsible,
        boolean collapsed,
        boolean ruler,
        List<ItemDraft> subItems) {

    public ItemDraft {
        // Ensure the list is immutable
        subItems = List.copyOf(subItems);
    }

    public boolean hasLogo() {
        return (logoLabel != null && !logoLabel.isBlank()) ||
                (logoImageUrl != null && !logoImageUrl.isBlank()) ||
                (logoLink != null && !logoLink.isBlank());
    }
}
