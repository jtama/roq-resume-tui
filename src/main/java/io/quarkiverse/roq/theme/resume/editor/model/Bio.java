package io.quarkiverse.roq.theme.resume.editor.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Bio(List<Section> list) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Section(@JsonProperty(required = true) String title, List<Item> items) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Item(@JsonProperty(required = true) String header, @JsonProperty(required = true) String title,
            String link, String content, Logo logo, Boolean collapsible, Boolean collapsed, Boolean ruler,
            List<Item> subItems) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Logo(@JsonProperty(required = true) String label, @JsonProperty(required = true) String imageUrl,
            String link) {
    }
}
