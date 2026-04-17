package io.keymate.accessrules.simulation;

import io.keymate.accessrules.cel.Decision;
import io.keymate.accessrules.cel.EvaluationResult;
import io.keymate.accessrules.cel.ConditionTrace;
import io.keymate.accessrules.matching.MatchRequest;
import io.keymate.accessrules.matching.MatchResult;
import io.keymate.accessrules.matching.RuleMatcher;
import io.keymate.accessrules.model.AccessRuleSet;
import io.keymate.accessrules.model.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Simulator {

    private static final Logger log = LoggerFactory.getLogger(Simulator.class);

    private final CelEvaluatorFunction celEvaluator;

    @FunctionalInterface
    public interface CelEvaluatorFunction {
        EvaluationResult evaluate(String celExpression, Map<String, Object> context);
    }

    public Simulator(CelEvaluatorFunction celEvaluator) {
        this.celEvaluator = celEvaluator;
    }

    public SimulationReport run(AccessRuleSet ruleSet, List<Scenario> scenarios) {
        log.debug("Running simulation with {} scenarios against policy '{}'",
                scenarios.size(), ruleSet.name());
        List<ScenarioResult> results = new ArrayList<>();
        for (Scenario scenario : scenarios) {
            results.add(runScenario(ruleSet, scenario));
        }
        SimulationReport report = SimulationReport.fromResults(results);
        log.debug("Simulation complete: {}/{} passed", report.passed(), report.totalScenarios());
        return report;
    }

    private ScenarioResult runScenario(AccessRuleSet ruleSet, Scenario scenario) {
        String name = scenario.name();

        try {
            Scenario.Request req = scenario.request();
            MatchRequest matchRequest = new MatchRequest(
                    req.sourceClientId(),
                    req.targetClientId(),
                    req.targetMethod(),
                    req.targetUri()
            );

            RuleMatcher matcher = new RuleMatcher(ruleSet);
            MatchResult matchResult = matcher.match(matchRequest);

            if (!matchResult.matched()) {
                return checkNoMatchExpected(name, scenario);
            }

            Rule matchedRule = matchResult.rule();
            String matchedRuleId = matchedRule.id();

            // Evaluate CEL with normalizedContext
            String celDecision = null;
            List<String> failedConditions = null;

            if (matchedRule.condition() != null && !matchedRule.condition().isBlank()) {
                Map<String, Object> context = scenario.normalizedContext() != null
                        ? scenario.normalizedContext()
                        : Collections.emptyMap();
                EvaluationResult evalResult = celEvaluator.evaluate(matchedRule.condition().trim(), context);
                celDecision = evalResult.decision().name();

                if (evalResult.decision() == Decision.DENY && evalResult.trace() != null) {
                    failedConditions = evalResult.trace().stream()
                            .filter(t -> "false".equals(t.result()))
                            .map(ConditionTrace::condition)
                            .toList();
                }
            }

            String finalDecision = celDecision != null ? celDecision : "GRANT";

            return compareResults(name, scenario, matchedRuleId, celDecision, finalDecision, failedConditions);

        } catch (Exception e) {
            log.warn("Unexpected error in scenario '{}': {}", name, e.getMessage());
            return new ScenarioResult(name, false, null, null, null, null, null,
                    "Unexpected error: " + e.getMessage());
        }
    }

    private ScenarioResult checkNoMatchExpected(String name, Scenario scenario) {
        Scenario.Expected expected = scenario.expected();
        if (expected == null || "NO_MATCH".equals(expected.matcherDecision())) {
            return new ScenarioResult(name, true, "NO_MATCH", null, null, "DENY", null, null);
        }
        return new ScenarioResult(name, false, "NO_MATCH", null, null, "DENY", null,
                "Expected MATCH but no rule matched");
    }

    private ScenarioResult compareResults(String name, Scenario scenario,
                                          String matchedRuleId, String celDecision,
                                          String finalDecision, List<String> failedConditions) {
        Scenario.Expected expected = scenario.expected();
        if (expected == null) {
            return new ScenarioResult(name, true, "MATCH", matchedRuleId,
                    celDecision, finalDecision, failedConditions, null);
        }

        List<String> failures = new ArrayList<>();

        if (expected.matchedRuleId() != null
                && !expected.matchedRuleId().equals(matchedRuleId)) {
            failures.add("Expected rule '" + expected.matchedRuleId()
                    + "' but got '" + matchedRuleId + "'");
        }

        if (expected.celDecision() != null
                && !expected.celDecision().equalsIgnoreCase(celDecision)) {
            failures.add("Expected CEL decision '" + expected.celDecision()
                    + "' but got '" + celDecision + "'");
        }

        if (expected.finalDecision() != null
                && !expected.finalDecision().equalsIgnoreCase(finalDecision)) {
            failures.add("Expected final decision '" + expected.finalDecision()
                    + "' but got '" + finalDecision + "'");
        }

        boolean passed = failures.isEmpty();
        String failureDetail = passed ? null : String.join("; ", failures);

        return new ScenarioResult(name, passed, "MATCH", matchedRuleId,
                celDecision, finalDecision, failedConditions, failureDetail);
    }
}
