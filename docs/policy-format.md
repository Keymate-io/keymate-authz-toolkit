# Policy Format

Policies are defined in YAML using the `AccessRuleSet` schema.

## AccessRuleSet

Top-level structure of a policy file.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | Yes | Unique identifier for this rule set |
| `kind` | string | Recommended | Should be `AccessRuleSet` |
| `version` | integer | Yes | Schema version, must be >= 1 |
| `defaults` | object | No | Global defaults (see below) |
| `rules` | list | Yes | At least one rule required |

### Defaults

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | boolean | `true` | Global enable/disable for rules |

## Rule

Each rule defines a match criteria and an optional condition expression.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Unique rule identifier (e.g. `agent.invoice.read`) |
| `description` | string | No | Human-readable description |
| `enabled` | boolean | No | Defaults to `true` if omitted. Set to `false` to skip this rule. |
| `match` | object | No | Routing-level match criteria |
| `methods` | list of strings | Yes | HTTP methods (e.g. `[GET, POST]`) |
| `targetUriPatterns` | list of strings | Yes | URI patterns with `*` wildcard support |
| `resources` | list of objects | No | Resource declarations |
| `condition` | string | No | CEL boolean expression for fine-grained evaluation |
| `intent` | string | Recommended | Expected outcome: `ALLOW` or `DENY` |

### Match

Routing-level criteria that determine which agent-to-service edge this rule applies to.

| Field | Type | Description |
|-------|------|-------------|
| `sourceClientId` | string | Exact match on the originating client (e.g. `invoice-agent`) |
| `targetClientId` | string | Exact match on the destination service (e.g. `invoice-api`) |

Both fields are optional. If omitted, the check is skipped (any value matches).

### Resources

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | Resource name (e.g. `invoice`) |
| `scopes` | list of strings | OAuth-style scopes (e.g. `[read, write]`) |

### URI Pattern Wildcards

The `*` wildcard matches any single path segment (does not cross `/`).

| Pattern | Matches | Does not match |
|---------|---------|---------------|
| `/api/v1/tenants/*/invoices/*` | `/api/v1/tenants/T-001/invoices/INV-123` | `/api/v1/tenants/T-001/sub/invoices/INV-123` |
| `/api/*/users` | `/api/v2/users` | `/api/v2/admin/users` |

## Example

```yaml
name: agent-invoice-read
kind: AccessRuleSet
version: 1

defaults:
  enabled: true

rules:
  - id: agent.invoice.read
    description: "Invoice read access for agent requests"
    enabled: true
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

## Validation Rules

The `validate` command checks:

| Check | Severity | Description |
|-------|----------|-------------|
| `name` missing | ERROR | `name` is required |
| `kind` missing | WARNING | `kind` is recommended |
| `version` missing or < 1 | ERROR | `version` must be >= 1 |
| No rules | ERROR | At least one rule is required |
| `id` missing | ERROR | Each rule must have an `id` |
| Duplicate `id` | ERROR | Rule IDs must be unique |
| No `methods` | ERROR | At least one HTTP method required |
| No `targetUriPatterns` | ERROR | At least one URI pattern required |
| `sourceClientId` missing | WARNING | Recommended when `match` is present |
| Invalid CEL syntax | ERROR | `condition` must be valid CEL |
| `intent` missing | WARNING | Recommended (`ALLOW` or `DENY`) |

A policy is valid if it has zero ERROR-severity findings. Warnings do not block validation.
