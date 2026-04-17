package io.keymate.accessrules.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that exercises the full kmctl authz pipeline:
 * validate → simulate → explain against the same policy and scenario files.
 */
@DisplayName("kmctl authz — full pipeline integration")
class AuthzIntegrationTest {

    private String policyPath;
    private String scenarioPath;

    @BeforeEach
    void setUp() {
        policyPath = resourcePath("policies/agent-invoice-policy.yaml");
        scenarioPath = resourcePath("scenarios/agent-invoice-scenarios.json");
    }

    @Nested
    @DisplayName("validate → simulate → explain sequential chain")
    class FullPipeline {

        @Test
        @DisplayName("policy that passes validation also passes simulation with all scenarios")
        void validateThenSimulate() {
            // Step 1: validate
            Result validate = execute("authz", "validate", policyPath);
            assertThat(validate.exitCode).as("validate exit code").isEqualTo(0);
            assertThat(validate.output).contains("Valid");

            // Step 2: simulate — same policy must produce 5/5
            Result simulate = execute("authz", "simulate", policyPath, "-s", scenarioPath);
            assertThat(simulate.exitCode).as("simulate exit code").isEqualTo(0);
            assertThat(simulate.output).contains("5/5 passed");
        }

        @Test
        @DisplayName("simulate decisions are consistent with explain traces")
        void simulateDecisionsMatchExplainTraces() {
            Result simulate = execute("authz", "simulate", policyPath, "-s", scenarioPath);
            Result explain = execute("authz", "explain", policyPath, "-s", scenarioPath);

            assertThat(simulate.exitCode).as("simulate exit code").isEqualTo(0);
            assertThat(explain.exitCode).as("explain exit code").isEqualTo(0);

            // grant scenario: both commands agree on GRANT
            assertThat(simulate.output).contains("grant-invoice-read → GRANT");
            assertThat(explain.output).contains("grant-invoice-read");
            assertThat(explain.output).contains("Decision: GRANT");

            // deny scenarios: both commands surface DENY
            assertThat(simulate.output).contains("deny-low-trust → DENY");
            assertThat(simulate.output).contains("deny-tenant-mismatch → DENY");
            assertThat(simulate.output).contains("deny-missing-actor-context → DENY");
            assertThat(simulate.output).contains("deny-high-risk → DENY");
            assertThat(explain.output).contains("Decision: DENY");
        }
    }

    @Nested
    @DisplayName("explain condition traces")
    class ExplainTraceConsistency {

        @Test
        @DisplayName("each deny scenario has its specific failed condition in explain trace")
        void explainTracesShowSpecificFailedConditions() {
            Result explain = execute("authz", "explain", policyPath, "-s", scenarioPath);

            assertThat(explain.exitCode).isEqualTo(0);

            // Each deny scenario must show the exact condition that caused denial
            assertThat(explain.output).contains("agent.trust_level >= 3");
            assertThat(explain.output).contains("tenant.id == resource.tenant_id");
            assertThat(explain.output).contains("actor.id != ''");
            assertThat(explain.output).contains("risk.score <= 5");
        }

        @Test
        @DisplayName("grant scenario has all conditions passing in explain trace")
        void grantScenarioAllConditionsPass() {
            Result explain = execute("authz", "explain", policyPath, "-s", scenarioPath);

            // Extract the grant-invoice-read block — between its header and the
            // next scenario header (deny-low-trust)
            String output = explain.output;
            int grantStart = output.indexOf("── grant-invoice-read ──");
            int nextScenario = output.indexOf("── deny-low-trust ──");
            String grantBlock = output.substring(grantStart, nextScenario);

            // All conditions in the grant block must be marked with ✓ (pass)
            assertThat(grantBlock).doesNotContain("✗");
            assertThat(grantBlock).contains("Decision: GRANT");
        }
    }

    @Nested
    @DisplayName("error propagation across pipeline")
    class ErrorPropagation {

        @Test
        @DisplayName("all three commands fail consistently for non-existent policy")
        void nonExistentPolicyFailsAllCommands() {
            String bogus = "/tmp/nonexistent-policy-" + System.nanoTime() + ".yaml";

            Result validate = execute("authz", "validate", bogus);
            Result simulate = execute("authz", "simulate", bogus, "-s", scenarioPath);
            Result explain = execute("authz", "explain", bogus, "-s", scenarioPath);

            assertThat(validate.exitCode).as("validate").isNotEqualTo(0);
            assertThat(simulate.exitCode).as("simulate").isNotEqualTo(0);
            assertThat(explain.exitCode).as("explain").isNotEqualTo(0);
        }

        @Test
        @DisplayName("simulate and explain fail consistently for non-existent scenario file")
        void nonExistentScenarioFailsBothCommands() {
            String bogus = "/tmp/nonexistent-scenarios-" + System.nanoTime() + ".json";

            Result simulate = execute("authz", "simulate", policyPath, "-s", bogus);
            Result explain = execute("authz", "explain", policyPath, "-s", bogus);

            assertThat(simulate.exitCode).as("simulate").isNotEqualTo(0);
            assertThat(explain.exitCode).as("explain").isNotEqualTo(0);
        }
    }

    // -- helpers --

    private record Result(int exitCode, String output) {}

    private static Result execute(String... args) {
        StringWriter out = new StringWriter();
        CommandLine cmd = new CommandLine(new AccessRulesCli());
        cmd.setOut(new PrintWriter(out));
        int exitCode = cmd.execute(args);
        return new Result(exitCode, out.toString());
    }

    private static String resourcePath(String resource) {
        URL url = AuthzIntegrationTest.class.getClassLoader().getResource(resource);
        if (url == null) {
            throw new IllegalStateException("Resource not found: " + resource);
        }
        return Path.of(url.getPath()).toString();
    }
}
