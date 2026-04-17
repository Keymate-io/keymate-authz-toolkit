package io.keymate.accessrules.simulation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScenarioFile(
        String policyId,
        List<Scenario> scenarios
) {}
