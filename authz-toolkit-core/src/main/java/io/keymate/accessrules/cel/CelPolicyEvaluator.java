package io.keymate.accessrules.cel;

import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CelPolicyEvaluator {

    private static final Logger log = LoggerFactory.getLogger(CelPolicyEvaluator.class);

    private static final Set<String> DECLARED_VARIABLES =
            Set.of("agent", "actor", "tenant", "user", "resource", "risk", "request");

    private static final Pattern ROOT_VAR_PATTERN =
            Pattern.compile("\\b(" + String.join("|", DECLARED_VARIABLES) + ")\\.");

    private final CelCompilerProvider compilerProvider;
    private final CelRuntime runtime;

    public CelPolicyEvaluator(CelCompilerProvider compilerProvider) {
        this.compilerProvider = compilerProvider;
        this.runtime = CelRuntimeFactory.standardCelRuntimeBuilder().build();
    }

    public EvaluationResult evaluate(String celExpression, Map<String, Object> context) {
        try {
            Map<String, Object> celContext = coerceNumericTypes(context);

            List<String> missing = findMissingVariables(celExpression, celContext);
            if (!missing.isEmpty()) {
                log.warn("Missing context variables for CEL evaluation: {}", missing);
                return EvaluationResult.unknown(
                        "Missing context variable(s): " + String.join(", ", missing));
            }

            CelAbstractSyntaxTree ast = compilerProvider.compile(celExpression);
            CelRuntime.Program program = runtime.createProgram(ast);
            Object result = program.eval(celContext);

            if (result instanceof Boolean boolResult) {
                Decision decision = boolResult ? Decision.GRANT : Decision.DENY;
                log.debug("CEL evaluation result: {}", decision);
                return boolResult ? EvaluationResult.grant() : EvaluationResult.deny();
            }
            log.warn("CEL expression returned non-boolean: {}", result);
            return EvaluationResult.unknown("CEL expression did not return boolean: " + result);
        } catch (Exception e) {
            log.warn("CEL evaluation error", e);
            return EvaluationResult.error("CEL evaluation error: " + e.getMessage());
        }
    }

    public EvaluationResult evaluateWithTrace(String celExpression, Map<String, Object> context) {
        EvaluationResult fullResult = evaluate(celExpression, context);

        if (fullResult.decision() == Decision.UNKNOWN || fullResult.decision() == Decision.ERROR) {
            return fullResult;
        }

        Map<String, Object> celContext = coerceNumericTypes(context);
        List<ConditionTrace> traces = new ArrayList<>();
        String[] conditions = celExpression.split("&&");

        for (String condition : conditions) {
            String trimmed = condition.trim();
            if (trimmed.isEmpty()) continue;

            List<String> missing = findMissingVariables(trimmed, celContext);
            if (!missing.isEmpty()) {
                traces.add(ConditionTrace.of(trimmed, "unknown",
                        "Missing: " + String.join(", ", missing)));
                continue;
            }

            try {
                CelAbstractSyntaxTree ast = compilerProvider.compile(trimmed);
                CelRuntime.Program program = runtime.createProgram(ast);
                Object result = program.eval(celContext);

                if (result instanceof Boolean boolResult) {
                    traces.add(ConditionTrace.of(trimmed, boolResult));
                } else {
                    traces.add(ConditionTrace.of(trimmed, "unknown",
                            "Non-boolean result: " + result));
                }
            } catch (Exception e) {
                traces.add(ConditionTrace.of(trimmed, "error", e.getMessage()));
            }
        }

        return fullResult.decision() == Decision.GRANT
                ? EvaluationResult.grantWithTrace(traces)
                : EvaluationResult.denyWithTrace(traces);
    }

    List<String> findMissingVariables(String celExpression, Map<String, Object> context) {
        List<String> missing = new ArrayList<>();
        Matcher m = ROOT_VAR_PATTERN.matcher(celExpression);
        while (m.find()) {
            String varName = m.group(1);
            if (!context.containsKey(varName) && !missing.contains(varName)) {
                missing.add(varName);
            }
        }
        return missing;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> coerceNumericTypes(Map<String, Object> map) {
        Map<String, Object> result = new HashMap<>(map.size());
        for (var entry : map.entrySet()) {
            result.put(entry.getKey(), coerceValue(entry.getValue()));
        }
        return result;
    }

    /**
     * Coerces Java numeric types to the two CEL-Java native types:
     * {@code Long} (CEL int) and {@code Double} (CEL double).
     */
    @SuppressWarnings("unchecked")
    private Object coerceValue(Object value) {
        if (value instanceof Long || value instanceof Double || value instanceof String
                || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Integer i) {
            return i.longValue();
        }
        if (value instanceof Short s) {
            return s.longValue();
        }
        if (value instanceof Byte b) {
            return b.longValue();
        }
        if (value instanceof BigInteger bi) {
            return bi.longValue();
        }
        if (value instanceof Float f) {
            return f.doubleValue();
        }
        if (value instanceof BigDecimal bd) {
            if (bd.stripTrailingZeros().scale() <= 0) {
                return bd.longValue();
            }
            return bd.doubleValue();
        }
        if (value instanceof Map<?, ?> m) {
            return coerceNumericTypes((Map<String, Object>) m);
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::coerceValue).toList();
        }
        return value;
    }
}
