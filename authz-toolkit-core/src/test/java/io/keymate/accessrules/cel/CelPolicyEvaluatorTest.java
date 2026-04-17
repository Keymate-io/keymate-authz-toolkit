package io.keymate.accessrules.cel;

import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CelPolicyEvaluatorTest {

    private static CelPolicyEvaluator evaluator;

    @BeforeAll
    static void setUp() {
        evaluator = new CelPolicyEvaluator(new CelCompilerProvider());
    }

    @Test
    @DisplayName("Missing context variable produces UNKNOWN, not ERROR")
    void testMissingContextVariableProducesUnknown() {
        String cel = "agent.trust_level >= 3";
        Map<String, Object> context = Map.of(
                "user", Map.of("role", "admin"),
                "risk", Map.of("score", 2L)
        );

        EvaluationResult result = evaluator.evaluate(cel, context);

        assertThat(result.decision()).isEqualTo(Decision.UNKNOWN);
        assertThat(result.errorMessage()).contains("agent");
    }

    @Test
    @DisplayName("Completely empty context produces UNKNOWN")
    void testEmptyContextProducesUnknown() {
        String cel = "agent.trust_level >= 3 && risk.score <= 5";
        Map<String, Object> context = Map.of();

        EvaluationResult result = evaluator.evaluate(cel, context);

        assertThat(result.decision()).isEqualTo(Decision.UNKNOWN);
    }

    @Test
    @DisplayName("Full agent context evaluates to GRANT")
    void testFullAgentContextGrant() {
        String cel = "agent.type == 'mcp-client' && agent.trust_level >= 3 && tenant.id == resource.tenant_id";
        Map<String, Object> context = Map.of(
                "agent", Map.of("type", "mcp-client", "trust_level", 3),
                "tenant", Map.of("id", "T-001"),
                "resource", Map.of("tenant_id", "T-001")
        );

        EvaluationResult result = evaluator.evaluate(cel, context);

        assertThat(result.decision()).isEqualTo(Decision.GRANT);
    }

    @Test
    @DisplayName("Full agent context evaluates to GRANT with trace")
    void testFullAgentContextGrantWithTrace() {
        String cel = "agent.type == 'mcp-client' && agent.trust_level >= 3 && tenant.id == resource.tenant_id";
        Map<String, Object> context = Map.of(
                "agent", Map.of("type", "mcp-client", "trust_level", 3),
                "tenant", Map.of("id", "T-001"),
                "resource", Map.of("tenant_id", "T-001")
        );

        EvaluationResult result = evaluator.evaluateWithTrace(cel, context);

        assertThat(result.decision()).isEqualTo(Decision.GRANT);
        assertThat(result.trace()).hasSize(3);
        assertThat(result.trace()).allMatch(t -> "true".equals(t.result()));
    }

    @Test
    @DisplayName("Low trust level evaluates to DENY with trace")
    void testLowTrustDenyWithTrace() {
        String cel = "agent.type == 'mcp-client' && agent.trust_level >= 3 && risk.score <= 5";
        Map<String, Object> context = Map.of(
                "agent", Map.of("type", "mcp-client", "trust_level", 2),
                "risk", Map.of("score", 4)
        );

        EvaluationResult result = evaluator.evaluateWithTrace(cel, context);

        assertThat(result.decision()).isEqualTo(Decision.DENY);
        assertThat(result.trace()).isNotNull();
        assertThat(result.trace()).anyMatch(t ->
                t.condition().contains("trust_level") && "false".equals(t.result()));
    }

    @Test
    @DisplayName("Tenant mismatch evaluates to DENY")
    void testTenantMismatchDeny() {
        String cel = "tenant.id == resource.tenant_id";
        Map<String, Object> context = Map.of(
                "tenant", Map.of("id", "T-001"),
                "resource", Map.of("tenant_id", "T-002")
        );

        EvaluationResult result = evaluator.evaluate(cel, context);

        assertThat(result.decision()).isEqualTo(Decision.DENY);
    }

    @Nested
    @DisplayName("Numeric coercion")
    class NumericCoercion {

        private static final String CEL = "risk.score >= 3";

        @Test
        @DisplayName("Short value is coerced to CEL int")
        void shortCoercion() {
            Map<String, Object> context = Map.of("risk", Map.of("score", (short) 4));
            assertThat(evaluator.evaluate(CEL, context).decision()).isEqualTo(Decision.GRANT);
        }

        @Test
        @DisplayName("Byte value is coerced to CEL int")
        void byteCoercion() {
            Map<String, Object> context = Map.of("risk", Map.of("score", (byte) 5));
            assertThat(evaluator.evaluate(CEL, context).decision()).isEqualTo(Decision.GRANT);
        }

        @Test
        @DisplayName("BigInteger value is coerced to CEL int")
        void bigIntegerCoercion() {
            Map<String, Object> context = Map.of("risk", Map.of("score", BigInteger.valueOf(3)));
            assertThat(evaluator.evaluate(CEL, context).decision()).isEqualTo(Decision.GRANT);
        }

        @Test
        @DisplayName("BigDecimal whole number is coerced to CEL int")
        void bigDecimalWholeNumberCoercion() {
            Map<String, Object> context = Map.of("risk", Map.of("score", new BigDecimal("4.00")));
            assertThat(evaluator.evaluate(CEL, context).decision()).isEqualTo(Decision.GRANT);
        }

        @Test
        @DisplayName("Float value is coerced to CEL double")
        void floatCoercion() {
            String cel = "risk.score > 2.5";
            Map<String, Object> context = Map.of("risk", Map.of("score", 3.0f));
            assertThat(evaluator.evaluate(cel, context).decision()).isEqualTo(Decision.GRANT);
        }

        @Test
        @DisplayName("BigDecimal fractional value is coerced to CEL double")
        void bigDecimalFractionalCoercion() {
            String cel = "risk.score > 2.5";
            Map<String, Object> context = Map.of("risk", Map.of("score", new BigDecimal("3.14")));
            assertThat(evaluator.evaluate(cel, context).decision()).isEqualTo(Decision.GRANT);
        }
    }

    @Test
    @DisplayName("Timestamp within business hours → GRANT")
    void testTimestampWithinBusinessHours() {
        String cel = "request.time.getHours('Europe/Istanbul') >= 9 && request.time.getHours('Europe/Istanbul') <= 17";

        Timestamp businessHour = Timestamp.newBuilder()
                .setSeconds(Instant.parse("2026-01-15T14:00:00Z").getEpochSecond())
                .build();

        Map<String, Object> context = Map.of(
                "request", Map.of("time", businessHour)
        );

        EvaluationResult result = evaluator.evaluate(cel, context);

        assertThat(result.decision()).isEqualTo(Decision.GRANT);
    }

    @Test
    @DisplayName("Timestamp outside business hours → DENY")
    void testTimestampOutsideBusinessHours() {
        String cel = "request.time.getHours('Europe/Istanbul') >= 9 && request.time.getHours('Europe/Istanbul') <= 17";

        Timestamp offHour = Timestamp.newBuilder()
                .setSeconds(Instant.parse("2026-01-15T22:00:00Z").getEpochSecond())
                .build();

        Map<String, Object> context = Map.of(
                "request", Map.of("time", offHour)
        );

        EvaluationResult result = evaluator.evaluate(cel, context);

        assertThat(result.decision()).isEqualTo(Decision.DENY);
    }
}
