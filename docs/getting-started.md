# Getting Started

## Prerequisites

- Java 17+
- Maven 3.8+

## Build

```bash
mvn clean package
```

This runs the full test suite and produces the CLI fat JAR under:

```
authz-toolkit-cli/target/keymate-authz-toolkit-cli-<version>.jar
```

The current version is defined in the root `pom.xml` (`<version>` element).

## Create an Alias

Use a shell glob so the alias survives version bumps:

```bash
alias kmctl="java -jar $(ls authz-toolkit-cli/target/keymate-authz-toolkit-cli-*.jar | head -n 1)"
```

Verify:

```bash
kmctl --help
```

## Quick Start

### 1. Write a Policy

Create `my-policy.yaml`:

```yaml
name: agent-invoice-read
kind: AccessRuleSet
version: 1

rules:
  - id: agent.invoice.read
    match:
      sourceClientId: invoice-agent
      targetClientId: invoice-api
    methods: [GET]
    targetUriPatterns:
      - /api/v1/tenants/*/invoices/*
    resources:
      - name: invoice
        scopes: [read]
    condition: >
      agent.type == 'mcp-client'
      && agent.trust_level >= 3
      && actor.id != ''
      && request.channel == 'AGENT'
      && tenant.id == resource.tenant_id
      && user.role == 'finance-manager'
      && risk.score <= 5
    intent: ALLOW
```

### 2. Validate

```bash
kmctl authz validate my-policy.yaml
```

```
  ✓ Valid — 1 rule
```

### 3. Write Scenarios

Create `my-scenarios.json` with test cases (see [Scenario Format](scenario-format.md)).

### 4. Simulate

```bash
kmctl authz simulate my-policy.yaml -s my-scenarios.json
```

```
  ✓ grant-invoice-read          → GRANT
  ✓ deny-low-trust              → DENY
  ✓ deny-tenant-mismatch        → DENY
  ✓ deny-missing-actor-context  → DENY
  ✓ deny-high-risk              → DENY

  5/5 passed
```

### 5. Explain

```bash
kmctl authz explain my-policy.yaml -s my-scenarios.json
```

```
  ── deny-low-trust ──
  Match:    ✓ agent.invoice.read
  Target:   invoice-api
  ✓ agent.type == 'mcp-client'
  ✗ agent.trust_level >= 3
  ✓ actor.id != ''
  ✓ request.channel == 'AGENT'
  ✓ tenant.id == resource.tenant_id
  ✓ user.role == 'finance-manager'
  ✓ risk.score <= 5
  Decision: DENY
```

## Project Structure

```
keymate-authz-toolkit/
  pom.xml                  # Parent POM
  authz-toolkit-core/      # Access Rule Engine — matching, CEL evaluation, validation, simulation
  authz-toolkit-cli/       # kmctl binary (validate, simulate, explain)
  docs/                    # Documentation
```

## Next Steps

- [Policy Format](policy-format.md) for the full AccessRuleSet specification
- [CLI Reference](cli-reference.md) for all commands, options, and exit codes
- [Scenario Format](scenario-format.md) for writing test scenarios
- [Condition Evaluation](condition-evaluation.md) for CEL expression details
