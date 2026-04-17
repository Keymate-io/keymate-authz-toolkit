package io.keymate.accessrules.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ExplainCommandTest {

    @Test
    @DisplayName("explain produces per-scenario trace output")
    void testExplainOutputContainsTrace() {
        String policyPath = resourcePath("policies/agent-invoice-policy.yaml");
        String scenarioPath = resourcePath("scenarios/agent-invoice-scenarios.json");

        StringWriter out = new StringWriter();
        CommandLine cmd = new CommandLine(new AccessRulesCli());
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("authz", "explain", policyPath, "-s", scenarioPath);

        assertThat(exitCode).isEqualTo(0);
        String output = out.toString();
        assertThat(output).contains("──");
        assertThat(output).contains("Match:");
        assertThat(output).contains("Decision:");
    }

    @Test
    @DisplayName("explain shows condition trace for denied scenarios")
    void testExplainShowsConditionTrace() {
        String policyPath = resourcePath("policies/agent-invoice-policy.yaml");
        String scenarioPath = resourcePath("scenarios/agent-invoice-scenarios.json");

        StringWriter out = new StringWriter();
        CommandLine cmd = new CommandLine(new AccessRulesCli());
        cmd.setOut(new PrintWriter(out));

        cmd.execute("authz", "explain", policyPath, "-s", scenarioPath);

        String output = out.toString();
        assertThat(output).contains("agent.trust_level");
        assertThat(output).contains("tenant.id");
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
