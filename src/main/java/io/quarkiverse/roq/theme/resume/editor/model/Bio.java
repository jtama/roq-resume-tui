package io.quarkiverse.roq.theme.resume.editor.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Bio(@JsonUnwrapped List<Section> list) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Section(
            @JsonIgnore Long id,
            @JsonProperty(required = true) String title,
            List<Item> items) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Item(
            @JsonIgnore Long id,
            @JsonProperty(required = true) String header,
            @JsonProperty(required = true) String title,
            String link,
            String content,
            Logo logo,
            Boolean collapsible,
            Boolean collapsed,
            Boolean ruler,
            List<String> tags,
            List<Item> subItems) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Logo(
            @JsonProperty(required = true) String label,
            @JsonProperty(required = true) String imageUrl,
            String link) {
    }
}
