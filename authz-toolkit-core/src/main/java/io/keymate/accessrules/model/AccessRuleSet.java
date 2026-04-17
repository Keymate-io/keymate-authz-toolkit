package io.keymate.accessrules.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccessRuleSet(
        @JsonProperty("name") String name,
        @JsonProperty("kind") String kind,
        @JsonProperty("version") Integer version,
        @JsonProperty("defaults") Defaults defaults,
        @JsonProperty("rules") List<Rule> rules
) {
}
