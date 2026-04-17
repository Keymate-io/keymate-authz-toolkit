package io.keymate.accessrules.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Resource(
        @JsonProperty("name") String name,
        @JsonProperty("scopes") List<String> scopes
) {
}
