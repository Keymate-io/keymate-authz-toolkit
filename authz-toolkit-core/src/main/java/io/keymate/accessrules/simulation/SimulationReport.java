package io.keymate.accessrules.simulation;

import java.util.List;

/**
 * Aggregated report for all simulation scenarios.
 *
 * @param results         individual scenario results
 * @param totalScenarios  total number of scenarios executed
 * @param passed          number of scenarios that passed
 * @param failed          number of scenarios that failed
 */
public record SimulationReport(
        List<ScenarioResult> results,
        int totalScenarios,
        int passed,
        int failed
) {

    /** Build a report from a list of results. */
    public static SimulationReport fromResults(List<ScenarioResult> results) {
        int passedCount = (int) results.stream().filter(ScenarioResult::passed).count();
        return new SimulationReport(
                results,
                results.size(),
                passedCount,
                results.size() - passedCount
        );
    }
}
