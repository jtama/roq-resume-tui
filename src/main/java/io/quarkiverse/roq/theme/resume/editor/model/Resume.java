package io.quarkiverse.roq.theme.resume.editor.model;

public record Resume(Long id, String slug, String title, Profile profile, Bio bio, Social social) {
}
