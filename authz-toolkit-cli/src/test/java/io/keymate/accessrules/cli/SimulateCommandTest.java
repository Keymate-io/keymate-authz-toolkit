package io.keymate.accessrules.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SimulateCommandTest {

    @Test
    @DisplayName("simulate with valid policy and scenarios returns 0 and reports all passed")
    void testSimulateReturnsZeroOnAllPassed() {
        String policyPath = resourcePath("policies/agent-invoice-policy.yaml");
        String scenarioPath = resourcePath("scenarios/agent-invoice-scenarios.json");

        StringWriter out = new StringWriter();
        CommandLine cmd = new CommandLine(new AccessRulesCli());
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("authz", "simulate", policyPath, "-s", scenarioPath);

        assertThat(exitCode).isEqualTo(0);
        String output = out.toString();
        assertThat(output).contains("5/5 passed");
    }

    @Test
    @DisplayName("simulate output contains scenario names and decisions")
    void testSimulateOutputContainsScenarioNames() {
        String policyPath = resourcePath("policies/agent-invoice-policy.yaml");
        String scenarioPath = resourcePath("scenarios/agent-invoice-scenarios.json");

        StringWriter out = new StringWriter();
        CommandLine cmd = new CommandLine(new AccessRulesCli());
        cmd.setOut(new PrintWriter(out));

        cmd.execute("authz", "simulate", policyPath, "-s", scenarioPath);

        String output = out.toString();
        assertThat(output).contains("GRANT");
        assertThat(output).contains("DENY");
    }

    private String resourcePath(String resource) {
        URL url = getClass().getClassLoader().getResource(resource);
        if (url == null) {
            throw new IllegalStateException("Resource not found: " + resource);
        }
        return Path.of(url.getPath()).toString();
    }
}
