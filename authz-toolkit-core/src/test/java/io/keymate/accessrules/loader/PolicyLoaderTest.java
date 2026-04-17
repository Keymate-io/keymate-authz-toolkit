package io.keymate.accessrules.loader;

import io.keymate.accessrules.model.AccessRuleSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyLoaderTest {

    private static final String VALID_YAML = """
            name: test-ruleset
            kind: AccessRuleSet
            version: 1
            rules: []
            """;

    @Test
    @DisplayName("Load from InputStream")
    void testLoadFromInputStream() throws Exception {
        InputStream is = new ByteArrayInputStream(VALID_YAML.getBytes(StandardCharsets.UTF_8));
        AccessRuleSet ruleSet = PolicyLoader.load(is);

        assertThat(ruleSet.name()).isEqualTo("test-ruleset");
        assertThat(ruleSet.kind()).isEqualTo("AccessRuleSet");
    }

    @Test
    @DisplayName("Load from String")
    void testLoadFromString() throws Exception {
        AccessRuleSet ruleSet = PolicyLoader.loadFromString(VALID_YAML);

        assertThat(ruleSet.name()).isEqualTo("test-ruleset");
        assertThat(ruleSet.version()).isEqualTo(1);
        assertThat(ruleSet.rules()).isEmpty();
    }

    @Test
    @DisplayName("Load from classpath resource")
    void testLoadFromClasspath() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("policies/agent-invoice-policy.yaml")) {
            assertThat(is).isNotNull();
            AccessRuleSet ruleSet = PolicyLoader.load(is);

            assertThat(ruleSet.name()).isEqualTo("agent-invoice-read");
            assertThat(ruleSet.rules()).hasSize(1);
            assertThat(ruleSet.rules().get(0).id()).isEqualTo("agent.invoice.read");
        }
    }

    @Test
    @DisplayName("Invalid YAML throws PolicyLoadException")
    void testInvalidYamlThrows() {
        String badYaml = "this is not: valid: yaml: [broken";

        assertThatThrownBy(() -> PolicyLoader.loadFromString(badYaml))
                .isInstanceOf(PolicyLoadException.class);
    }
}
