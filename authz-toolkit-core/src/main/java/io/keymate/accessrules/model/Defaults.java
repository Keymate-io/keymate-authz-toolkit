package io.keymate.accessrules.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Defaults(
        @JsonProperty("enabled") boolean enabled
) {
}
