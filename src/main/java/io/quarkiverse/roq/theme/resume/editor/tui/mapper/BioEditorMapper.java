package io.quarkiverse.roq.theme.resume.editor.tui.mapper;

import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.roq.theme.resume.editor.model.Bio;
import io.quarkiverse.roq.theme.resume.editor.tui.draft.ItemDraft;
import io.quarkiverse.roq.theme.resume.editor.tui.draft.SectionDraft;

@ApplicationScoped
public class BioEditorMapper {

    public SectionDraft toDraft(Bio.Section section) {
        List<ItemDraft> items = section.items() != null
                ? section.items().stream().map(this::toDraft).toList()
                : Collections.emptyList();
        return new SectionDraft(section.title(), items);
    }

    public ItemDraft toDraft(Bio.Item item) {
        Bio.Logo logo = item.logo();
        List<ItemDraft> subItems = item.subItems() != null
                ? item.subItems().stream().map(this::toDraft).toList()
                : Collections.emptyList();

        return new ItemDraft(
                item.header(),
                item.title(),
                item.link(),
                item.content(),
                logo != null ? logo.label() : null,
                logo != null ? logo.imageUrl() : null,
                logo != null ? logo.link() : null,
                Boolean.TRUE.equals(item.collapsible()),
                Boolean.TRUE.equals(item.collapsed()),
                Boolean.TRUE.equals(item.ruler()),
                subItems);
    }

    public Bio.Section toSection(SectionDraft section) {
        List<Bio.Item> items = section.items().stream().map(this::toItem).toList();
        return new Bio.Section(trimToDefault(section.title(), "Untitled section"), items);
    }

    public Bio.Item toItem(ItemDraft item) {
        Bio.Logo logo = item.hasLogo()
                ? new Bio.Logo(item.logoLabel(), item.logoImageUrl(), item.logoLink())
                : null;
        List<Bio.Item> subItems = item.subItems().stream().map(this::toItem).toList();
        return new Bio.Item(
                trimToNull(item.header()),
                trimToDefault(item.title(), "Untitled item"),
                trimToNull(item.link()),
                trimToNull(item.content()),
                logo,
                item.collapsible(),
                item.collapsed(),
                item.ruler(),
                subItems);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed != null ? trimmed : defaultValue;
    }
}
