package io.keymate.accessrules.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RuleMatch(
        @JsonProperty("sourceClientId") String sourceClientId,
        @JsonProperty("targetClientId") String targetClientId
) {
}
