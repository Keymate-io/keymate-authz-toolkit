package io.keymate.accessrules.matching;

import io.keymate.accessrules.loader.PolicyLoader;
import io.keymate.accessrules.model.AccessRuleSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class RuleMatcherTest {

    private static RuleMatcher matcher;

    @BeforeAll
    static void setUp() throws Exception {
        try (InputStream is = RuleMatcherTest.class.getClassLoader().getResourceAsStream("policies/agent-invoice-policy.yaml")) {
            AccessRuleSet ruleSet = PolicyLoader.load(is);
            matcher = new RuleMatcher(ruleSet);
        }
    }

    @Test
    @DisplayName("Correct sourceClientId, method and URI matches")
    void testValidRequestMatches() {
        MatchRequest request = new MatchRequest(
                "invoice-agent", "invoice-api", "GET", "/api/v1/tenants/T-001/invoices/INV-123");

        MatchResult result = matcher.match(request);

        assertThat(result.matched()).isTrue();
        assertThat(result.rule().id()).isEqualTo("agent.invoice.read");
    }

    @Test
    @DisplayName("Wrong sourceClientId does not match")
    void testWrongClientNoMatch() {
        MatchRequest request = new MatchRequest(
                "unknown-agent", "invoice-api", "GET", "/api/v1/tenants/T-001/invoices/INV-123");

        MatchResult result = matcher.match(request);

        assertThat(result.matched()).isFalse();
    }

    @Test
    @DisplayName("Wrong HTTP method does not match")
    void testWrongMethodNoMatch() {
        MatchRequest request = new MatchRequest(
                "invoice-agent", "invoice-api", "POST", "/api/v1/tenants/T-001/invoices/INV-123");

        MatchResult result = matcher.match(request);

        assertThat(result.matched()).isFalse();
    }

    @Test
    @DisplayName("Wrong URI path does not match")
    void testWrongPathNoMatch() {
        MatchRequest request = new MatchRequest(
                "invoice-agent", "invoice-api", "GET", "/api/v1/tenants/T-001/orders/ORD-1");

        MatchResult result = matcher.match(request);

        assertThat(result.matched()).isFalse();
    }

    @Test
    @DisplayName("URI with different tenant segment still matches pattern")
    void testDifferentTenantMatches() {
        MatchRequest request = new MatchRequest(
                "invoice-agent", "invoice-api", "GET", "/api/v1/tenants/T-999/invoices/INV-001");

        MatchResult result = matcher.match(request);

        assertThat(result.matched()).isTrue();
    }

    @Test
    @DisplayName("No-arg matcher with explicit rules list")
    void testNoArgMatcherWithRulesList() throws Exception {
        RuleMatcher noArgMatcher = new RuleMatcher();
        try (InputStream is = RuleMatcherTest.class.getClassLoader().getResourceAsStream("policies/agent-invoice-policy.yaml")) {
            AccessRuleSet ruleSet = PolicyLoader.load(is);

            MatchRequest request = new MatchRequest(
                    "invoice-agent", "invoice-api", "GET", "/api/v1/tenants/T-001/invoices/INV-123");

            MatchResult result = noArgMatcher.match(request, ruleSet.rules());
            assertThat(result.matched()).isTrue();
        }
    }

    @Test
    @DisplayName("Repeated calls return consistent results — pattern cache must be safe")
    void testRepeatedCallsConsistent() {
        MatchRequest request = new MatchRequest(
                "invoice-agent", "invoice-api", "GET", "/api/v1/tenants/T-001/invoices/INV-123");

        for (int i = 0; i < 1_000; i++) {
            MatchResult result = matcher.match(request);
            assertThat(result.matched()).isTrue();
            assertThat(result.rule().id()).isEqualTo("agent.invoice.read");
        }
    }

    @Test
    @DisplayName("Matched rule has correct targetClientId")
    void testMatchedRuleTargetClient() {
        MatchRequest request = new MatchRequest(
                "invoice-agent", "invoice-api", "GET", "/api/v1/tenants/T-001/invoices/INV-123");

        MatchResult result = matcher.match(request);

        assertThat(result.matched()).isTrue();
        assertThat(result.rule().match().targetClientId()).isEqualTo("invoice-api");
    }
}
