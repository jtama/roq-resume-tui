package io.quarkiverse.roq.theme.resume.editor.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Profile(@JsonProperty(required = true) String firstName, @JsonProperty(required = true) String lastName,
        String picture, String jobTitle, String bio, String city, String country, String phone, String email,
        String site) {
}
