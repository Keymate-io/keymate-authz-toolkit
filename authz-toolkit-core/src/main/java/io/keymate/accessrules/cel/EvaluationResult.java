package io.keymate.accessrules.cel;

import java.util.List;

/**
 * Result of evaluating a CEL policy expression, including the decision
 * and an optional trace of individual sub-conditions (for why-denied analysis).
 *
 * @param decision     the overall decision (GRANT, DENY, UNKNOWN, ERROR)
 * @param trace        per-condition trace entries; empty list when why-denied analysis is not requested
 * @param errorMessage human-readable error message, null when no error occurred
 */
public record EvaluationResult(
        Decision decision,
        List<ConditionTrace> trace,
        String errorMessage
) {

    /** Shorthand for a simple GRANT with no trace. */
    public static EvaluationResult grant() {
        return new EvaluationResult(Decision.GRANT, List.of(), null);
    }

    /** GRANT with per-condition trace. */
    public static EvaluationResult grantWithTrace(List<ConditionTrace> trace) {
        return new EvaluationResult(Decision.GRANT, trace, null);
    }

    /** Shorthand for a simple DENY with no trace. */
    public static EvaluationResult deny() {
        return new EvaluationResult(Decision.DENY, List.of(), null);
    }

    /** DENY with why-denied trace. */
    public static EvaluationResult denyWithTrace(List<ConditionTrace> trace) {
        return new EvaluationResult(Decision.DENY, trace, null);
    }

    /** ERROR with a message. */
    public static EvaluationResult error(String message) {
        return new EvaluationResult(Decision.ERROR, List.of(), message);
    }

    /** UNKNOWN result. */
    public static EvaluationResult unknown(String detail) {
        return new EvaluationResult(Decision.UNKNOWN, List.of(), detail);
    }
}
