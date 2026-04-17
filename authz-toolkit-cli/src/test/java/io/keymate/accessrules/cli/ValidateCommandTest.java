package io.keymate.accessrules.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ValidateCommandTest {

    @Test
    @DisplayName("validate valid policy prints OK and returns 0")
    void testValidPolicyReturnsZero() {
        String policyPath = resourcePath("policies/agent-invoice-policy.yaml");

        StringWriter out = new StringWriter();
        CommandLine cmd = new CommandLine(new AccessRulesCli());
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("authz", "validate", policyPath);

        assertThat(exitCode).isEqualTo(0);
        assertThat(out.toString()).contains("Valid");
    }

    @Test
    @DisplayName("validate non-existent file returns non-zero")
    void testNonExistentFileReturnsNonZero() {
        StringWriter out = new StringWriter();
        CommandLine cmd = new CommandLine(new AccessRulesCli());
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("authz", "validate", "/tmp/nonexistent-policy.yaml");

        assertThat(exitCode).isNotEqualTo(0);
    }

    private String resourcePath(String resource) {
        URL url = getClass().getClassLoader().getResource(resource);
        if (url == null) {
            throw new IllegalStateException("Resource not found: " + resource);
        }
        return Path.of(url.getPath()).toString();
    }
}
