package io.quarkiverse.roq.theme.resume.editor.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Social(List<Item> items) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Item(@JsonProperty(required = true) String name, @JsonProperty(required = true) String url) {
    }
}
