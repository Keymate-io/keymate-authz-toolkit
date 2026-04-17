# CLI Reference

## Command Hierarchy

```
kmctl
  authz
    validate   Validate policy YAML and CEL syntax
    simulate   Run scenarios against a policy
    explain    Trace per-condition evaluation for each scenario
```

## kmctl authz validate

Validates a policy YAML file for structural correctness and CEL syntax.

### Usage

```bash
kmctl authz validate <policyFile>
```

### Arguments

| Argument | Description |
|----------|-------------|
| `<policyFile>` | Path to the policy YAML file |

### Output

On success:
```
  ✓ Valid — 1 rule
```

With warnings:
```
  ⚠ [kind] kind is recommended
  ✓ Valid — 1 rule, 1 warning(s)
```

On validation errors:
```
  ✗ [rules[0].id] rule id is required
```

On file not found:
```
  ✗ File not found — /path/to/missing.yaml
```

### Exit Codes

| Code | Meaning |
|------|---------|
| `0` | Policy is valid (warnings allowed) |
| `1` | Validation error, file not found, or parse error |

---

## kmctl authz simulate

Runs all scenarios in a JSON file against a policy and reports pass/fail per scenario.

### Usage

```bash
kmctl authz simulate <policyFile> -s <scenarioFile>
```

### Arguments

| Argument | Description |
|----------|-------------|
| `<policyFile>` | Path to the policy YAML file |

### Options

| Option | Required | Description |
|--------|----------|-------------|
| `--scenario`, `-s` | Yes | Path to the scenario JSON file |

### Output

```
  ✓ grant-invoice-read          → GRANT
  ✓ deny-low-trust              → DENY
  ✗ deny-tenant-mismatch        → DENY
    reason: Expected GRANT but got DENY

  4/5 passed
```

### Exit Codes

| Code | Meaning |
|------|---------|
| `0` | All scenarios passed |
| `1` | One or more scenarios failed, file not found, or error |

### What is Compared

For each scenario, the simulator compares:

- `matchedRuleId` (if specified in expected)
- `celDecision` (case-insensitive, if specified)
- `finalDecision` (case-insensitive, if specified)

---

## kmctl authz explain

For each scenario, traces which CEL sub-conditions passed or failed.

### Usage

```bash
kmctl authz explain <policyFile> -s <scenarioFile>
```

### Arguments

| Argument | Description |
|----------|-------------|
| `<policyFile>` | Path to the policy YAML file |

### Options

| Option | Required | Description |
|--------|----------|-------------|
| `--scenario`, `-s` | Yes | Path to the scenario JSON file |

### Output

When a rule matches and condition is denied:
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

When a rule matches and condition is granted:
```
  ── grant-invoice-read ──
  Match:    ✓ agent.invoice.read
  Target:   invoice-api
  ✓ agent.type == 'mcp-client'
  ✓ agent.trust_level >= 3
  ✓ actor.id != ''
  ✓ request.channel == 'AGENT'
  ✓ tenant.id == resource.tenant_id
  ✓ user.role == 'finance-manager'
  ✓ risk.score <= 5
  Decision: GRANT
```

When no rule matches:
```
  ── unknown-scenario ──
  Match:    ✗ No rule matched
  Reason:   No rule matched the request
  Decision: NO_MATCH
```

### Exit Codes

| Code | Meaning |
|------|---------|
| `0` | Always (explain does not fail on DENY decisions) |
| `1` | File not found or exception |

### Trace Behavior

Per-condition trace is produced for both **GRANT** and **DENY** decisions, so a granted request can be explained clause by clause just as a denied one can. Trace is suppressed only when the overall result is `UNKNOWN` (missing context variable) or `ERROR` (evaluation exception).
