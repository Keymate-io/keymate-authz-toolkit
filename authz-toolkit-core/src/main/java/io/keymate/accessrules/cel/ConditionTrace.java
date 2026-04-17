package io.keymate.accessrules.cel;

/**
 * Trace entry for a single sub-condition within a policy expression.
 * Used in why-denied analysis to show which parts passed or failed.
 *
 * @param condition the CEL sub-expression that was evaluated
 * @param result    "true", "false", "unknown", or "error"
 * @param detail    optional extra information such as actual variable values
 */
public record ConditionTrace(
        String condition,
        String result,
        String detail
) {

    /**
     * Convenience factory for a successfully evaluated condition.
     */
    public static ConditionTrace of(String condition, boolean result) {
        return new ConditionTrace(condition, String.valueOf(result), null);
    }

    /**
     * Convenience factory with detail.
     */
    public static ConditionTrace of(String condition, String result, String detail) {
        return new ConditionTrace(condition, result, detail);
    }
}
