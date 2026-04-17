package io.keymate.accessrules.simulation;

import java.util.List;

public record ScenarioResult(
        String scenarioName,
        boolean passed,
        String matcherDecision,
        String matchedRuleId,
        String celDecision,
        String finalDecision,
        List<String> failedConditions,
        String failureDetail
) {
}
