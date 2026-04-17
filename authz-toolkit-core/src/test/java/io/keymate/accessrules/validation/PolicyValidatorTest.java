package io.keymate.accessrules.validation;

import io.keymate.accessrules.cel.CelCompilerProvider;
import io.keymate.accessrules.loader.PolicyLoader;
import io.keymate.accessrules.model.AccessRuleSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyValidatorTest {

    private static PolicyValidator validator;

    @BeforeAll
    static void setUp() {
        validator = new PolicyValidator(new CelCompilerProvider());
    }

    @Test
    @DisplayName("agent-invoice-policy.yaml passes all validations")
    void testValidPolicyPasses() throws Exception {
        AccessRuleSet ruleSet = loadResource("policies/agent-invoice-policy.yaml");
        ValidationReport report = validator.validate(ruleSet);

        assertThat(report.errors())
                .as("Errors: " + report.errors())
                .isEmpty();
        assertThat(report.isValid()).isTrue();
    }

    @Nested
    @DisplayName("Required fields")
    class RequiredFields {

        @Test
        @DisplayName("missing name → error")
        void testMissingName() throws Exception {
            AccessRuleSet ruleSet = loadYaml("""
                    kind: AccessRuleSet
                    version: 1
                    rules:
                      - id: r1
                        methods: [GET]
                        targetUriPatterns: [/api/test]
                    """);
            ValidationReport report = validator.validate(ruleSet);

            assertThat(report.errors()).anyMatch(e ->
                    e.path().equals("name") && e.message().contains("required"));
        }

        @Test
        @DisplayName("missing rule id → error")
        void testMissingRuleId() throws Exception {
            AccessRuleSet ruleSet = loadYaml("""
                    name: test
                    kind: AccessRuleSet
                    rules:
                      - methods: [GET]
                        targetUriPatterns: [/api/test]
                    """);
            ValidationReport report = validator.validate(ruleSet);

            assertThat(report.errors()).anyMatch(e ->
                    e.path().contains("rules[0].id"));
        }

        @Test
        @DisplayName("missing methods → error")
        void testMissingMethods() throws Exception {
            AccessRuleSet ruleSet = loadYaml("""
                    name: test
                    kind: AccessRuleSet
                    rules:
                      - id: r1
                        targetUriPatterns: [/api/test]
                    """);
            ValidationReport report = validator.validate(ruleSet);

            assertThat(report.errors()).anyMatch(e ->
                    e.path().contains("methods") && e.message().contains("required"));
        }

        @Test
        @DisplayName("missing targetUriPatterns → error")
        void testMissingTargetUriPatterns() throws Exception {
            AccessRuleSet ruleSet = loadYaml("""
                    name: test
                    kind: AccessRuleSet
                    rules:
                      - id: r1
                        methods: [GET]
                    """);
            ValidationReport report = validator.validate(ruleSet);

            assertThat(report.errors()).anyMatch(e ->
                    e.path().contains("targetUriPatterns") && e.message().contains("required"));
        }

        @Test
        @DisplayName("no rules → error")
        void testNoRules() throws Exception {
            AccessRuleSet ruleSet = loadYaml("""
                    name: test
                    kind: AccessRuleSet
                    rules: []
                    """);
            ValidationReport report = validator.validate(ruleSet);

            assertThat(report.errors()).anyMatch(e ->
                    e.path().equals("rules") && e.message().contains("at least one"));
        }
    }

    @Nested
    @DisplayName("Uniqueness")
    class Uniqueness {

        @Test
        @DisplayName("duplicate rule ids → error")
        void testDuplicateRuleIds() throws Exception {
            AccessRuleSet ruleSet = loadYaml("""
                    name: test
                    kind: AccessRuleSet
                    rules:
                      - id: same-id
                        methods: [GET]
                        targetUriPatterns: [/api/a]
                      - id: same-id
                        methods: [POST]
                        targetUriPatterns: [/api/b]
                    """);
            ValidationReport report = validator.validate(ruleSet);

            assertThat(report.errors()).anyMatch(e ->
                    e.message().contains("duplicate rule id"));
        }
    }

    @Nested
    @DisplayName("CEL validation")
    class CelValidation {

        @Test
        @DisplayName("invalid CEL syntax → error")
        void testInvalidCelSyntax() throws Exception {
            AccessRuleSet ruleSet = loadYaml("""
                    name: test
                    kind: AccessRuleSet
                    rules:
                      - id: r1
                        methods: [GET]
                        targetUriPatterns: [/api/test]
                        condition: "agent.type == && invalid"
                    """);
            ValidationReport report = validator.validate(ruleSet);

            assertThat(report.errors()).anyMatch(e ->
                    e.path().contains("condition"));
        }

        @Test
        @DisplayName("valid CEL expression → no error")
        void testValidCel() throws Exception {
            AccessRuleSet ruleSet = loadYaml("""
                    name: test
                    kind: AccessRuleSet
                    rules:
                      - id: r1
                        methods: [GET]
                        targetUriPatterns: [/api/test]
                        condition: "agent.trust_level >= 3 && risk.score <= 5"
                    """);
            ValidationReport report = validator.validate(ruleSet);

            assertThat(report.errors().stream()
                    .filter(e -> e.path().contains("condition"))
                    .toList()).isEmpty();
        }
    }

    private static AccessRuleSet loadResource(String path) throws Exception {
        try (InputStream is = PolicyValidatorTest.class.getClassLoader().getResourceAsStream(path)) {
            return PolicyLoader.load(is);
        }
    }

    private static AccessRuleSet loadYaml(String yaml) throws Exception {
        return PolicyLoader.loadFromString(yaml);
    }
}
