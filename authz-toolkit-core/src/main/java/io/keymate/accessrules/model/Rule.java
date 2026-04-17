package io.keymate.accessrules.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Rule(
        @JsonProperty("id") String id,
        @JsonProperty("description") String description,
        @JsonProperty("enabled") Boolean enabled,
        @JsonProperty("match") RuleMatch match,
        @JsonProperty("methods") List<String> methods,
        @JsonProperty("targetUriPatterns") List<String> targetUriPatterns,
        @JsonProperty("resources") List<Resource> resources,
        @JsonProperty("condition") String condition,
        @JsonProperty("intent") String intent
) {

    public boolean isEnabled() {
        return enabled == null || enabled;
    }
}
