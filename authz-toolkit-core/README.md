# keymate-authz-toolkit-core

Embeddable Java library that powers [kmctl](../README.md) — the Access Rule Engine behind `AccessRuleSet` policies.

Use this module if you want to evaluate Access Rules **inside your own JVM application** (gateway, policy decision point, admin tooling) without shelling out to the CLI.

For CLI usage, see [`authz-toolkit-cli`](../authz-toolkit-cli/README.md).

## Installation

```xml
<dependency>
    <groupId>io.keymate</groupId>
    <artifactId>keymate-authz-toolkit-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

Requires **Java 17+**.

## Public API

| Package | Purpose |
|---------|---------|
| `io.keymate.accessrules.model` | `AccessRuleSet`, `Rule`, `RuleMatch`, `Resource`, `Defaults` — immutable records that map 1:1 to the policy YAML. |
| `io.keymate.accessrules.loader` | `PolicyLoader` — parses policy YAML (from a file path or a string) into an `AccessRuleSet`. |
| `io.keymate.accessrules.validation` | `PolicyValidator`, `ValidationReport`, `ValidationError` — structural and CEL syntax validation. |
| `io.keymate.accessrules.matching` | `RuleMatcher`, `MatchRequest`, `MatchResult` — picks the first rule whose `match` block applies to a request. Thread-safe, pre-compiles URI patterns. |
| `io.keymate.accessrules.cel` | `CelCompilerProvider`, `CelPolicyEvaluator`, `EvaluationResult`, `Decision`, `ConditionTrace` — compiles and evaluates CEL `condition` expressions. Coerces Java numeric types to CEL native `Long` / `Double`. |
| `io.keymate.accessrules.simulation` | `Scenario`, `ScenarioFile`, `Simulator`, `ScenarioResult`, `SimulationReport` — runs a list of scenarios against a policy. |

## Embedding Example

```java
import io.keymate.accessrules.cel.*;
import io.keymate.accessrules.loader.PolicyLoader;
import io.keymate.accessrules.matching.*;
import io.keymate.accessrules.model.AccessRuleSet;
import io.keymate.accessrules.model.Rule;

import java.nio.file.Path;
import java.util.Map;

AccessRuleSet ruleSet = PolicyLoader.load(Path.of("agent-invoice-policy.yaml"));

RuleMatcher matcher = new RuleMatcher(ruleSet);
CelPolicyEvaluator evaluator = new CelPolicyEvaluator(new CelCompilerProvider());

MatchRequest request = new MatchRequest(
        "invoice-agent",          // sourceClientId
        "invoice-api",            // targetClientId
        "GET",                    // HTTP method
        "/api/v1/tenants/T-001/invoices/INV-123"
);

MatchResult match = matcher.match(request);
if (!match.matched()) {
    // No rule applies — decide your own fallback (deny, delegate, etc.)
    return;
}

Rule rule = match.rule();
if (rule.condition() == null || rule.condition().isBlank()) {
    // Rule matched without a condition → GRANT
    return;
}

Map<String, Object> context = Map.of(
        "agent",    Map.of("type", "mcp-client", "trust_level", 3),
        "actor",    Map.of("id", "user-789"),
        "tenant",   Map.of("id", "T-001"),
        "user",     Map.of("role", "finance-manager"),
        "risk",     Map.of("score", 4),
        "resource", Map.of("tenant_id", "T-001"),
        "request",  Map.of("channel", "AGENT")
);

EvaluationResult result = evaluator.evaluateWithTrace(rule.condition(), context);

switch (result.decision()) {
    case GRANT -> { /* allow */ }
    case DENY  -> result.trace().forEach(t ->
            System.out.println(("true".equals(t.result()) ? "✓ " : "✗ ") + t.condition()));
    case UNKNOWN, ERROR -> { /* fail closed */ }
}
```

## What This Module Does Not Do

- **No CLI, no user-facing I/O.** This module is pure library code. The CLI lives in [`authz-toolkit-cli`](../authz-toolkit-cli/README.md).
- **No token handling.** Matching and evaluation operate on already-extracted request metadata and a pre-normalized context map — not JWTs. Integration with a specific token format is the caller's responsibility.
- **No authority decisions.** If a condition evaluates to `GRANT`, that does not necessarily mean the final authorization is allowed; typical deployments still consult an external authority (e.g. Keycloak) afterwards.

## Documentation

- [Policy Format](../docs/policy-format.md)
- [Rule Matching](../docs/matching.md)
- [Condition Evaluation](../docs/condition-evaluation.md)
- [Scenario Format](../docs/scenario-format.md)

## License

Apache License 2.0 — see the project-level [LICENSE](../LICENSE) and [NOTICE](../NOTICE).
