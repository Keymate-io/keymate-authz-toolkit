# keymate-authz-toolkit-cli

The **`kmctl`** command-line binary — a picocli-based front-end to the [Access Rule Engine](../authz-toolkit-core/README.md).

`kmctl authz` gives policy authors three commands to close the loop between designing, testing, and explaining an `AccessRuleSet`:

| Command | Purpose |
|---------|---------|
| `validate` | Structural and CEL syntax check of a policy YAML. |
| `simulate` | Runs scenarios against a policy and reports pass/fail. |
| `explain`  | Per-condition trace (GRANT and DENY) so reviewers can see exactly which clause passed or failed. |

## Build

From the project root:

```bash
mvn clean package
```

This produces a self-contained fat JAR under:

```
authz-toolkit-cli/target/keymate-authz-toolkit-cli-<version>.jar
```

## Run

Create an alias so the command is available in your shell:

```bash
alias kmctl="java -jar $(ls authz-toolkit-cli/target/keymate-authz-toolkit-cli-*.jar | head -n 1)"
```

Then:

```bash
kmctl authz validate my-policy.yaml
kmctl authz simulate my-policy.yaml -s my-scenarios.json
kmctl authz explain  my-policy.yaml -s my-scenarios.json
```

## Example Output

```
$ kmctl authz simulate agent-invoice-policy.yaml -s agent-invoice-scenarios.json
  ✓ grant-invoice-read          → GRANT
  ✓ deny-low-trust              → DENY
  ✓ deny-tenant-mismatch        → DENY
  ✓ deny-missing-actor-context  → DENY
  ✓ deny-high-risk              → DENY

  5/5 passed
```

```
$ kmctl authz explain agent-invoice-policy.yaml -s agent-invoice-scenarios.json
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

## Exit Codes

| Code | Meaning |
|------|---------|
| `0` | Success (or, for `explain`, always on completion) |
| `1` | Validation failed, scenario failed, file not found, or unexpected error |

These codes make `kmctl authz` easy to wire into CI pipelines (fail the build on a broken policy or regressed scenario).

## Embedded Policy and Scenarios

The test and resource directories of this module contain the reference `agent-invoice-policy.yaml` and `agent-invoice-scenarios.json` used as a demo.

## Documentation

- [Getting Started](../docs/getting-started.md) — end-to-end walkthrough.
- [CLI Reference](../docs/cli-reference.md) — every command, option, and exit code.
- [Policy Format](../docs/policy-format.md) and [Scenario Format](../docs/scenario-format.md).

## License

Apache License 2.0 — see the project-level [LICENSE](../LICENSE) and [NOTICE](../NOTICE).
