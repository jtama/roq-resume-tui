package io.quarkiverse.roq.theme.resume.editor.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Resume(Long id, String slug, String title, Profile profile, Social social, Bio bio) {
}
