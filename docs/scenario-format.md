# Scenario Format

Scenarios are defined in JSON and used by the `simulate` and `explain` commands to test policies.

## File Structure

```json
{
  "policyId": "agent.invoice.read",
  "scenarios": [ ... ]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `policyId` | string | References the policy rule ID this file targets |
| `scenarios` | array | List of test scenarios |

## Scenario

Each scenario defines a request, context, and expected outcome.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | Yes | Unique scenario identifier (shown in output) |
| `description` | string | No | Human-readable description |
| `request` | object | Yes | Simulated gateway request |
| `tokenClaims` | object | No | Raw JWT claims (informational, not used by engine) |
| `normalizedContext` | object | Yes | The evaluation context passed to CEL |
| `expected` | object | Yes | Expected outcome for assertion |

## Request

The request object simulates the incoming gateway call. Match fields are extracted from headers.

| Field | Type | Description |
|-------|------|-------------|
| `gatewayMethod` | string | HTTP method at gateway level (e.g. `POST`) |
| `gatewayPath` | string | Gateway endpoint path |
| `headers` | object | HTTP headers containing routing signals |
| `body` | object | Optional request body |

### Header-to-Match Mapping

| Header | Maps to | Used for |
|--------|---------|----------|
| `Keymate-Client-Id` | `sourceClientId` | Which client is calling |
| `Keymate-Target-Client-Id` | `targetClientId` | Which service is being called |
| `Keymate-Target-Method` | `method` | HTTP method on target resource |
| `Keymate-Target-URI` | `targetUri` | URI on target resource |

## Normalized Context

The `normalizedContext` is a nested map passed directly to CEL evaluation. It must contain the context variables referenced by the policy's `condition` expression.

### Available Context Variables

| Variable | Type | Description |
|----------|------|-------------|
| `agent` | map | Agent properties: `id`, `type`, `trust_level` |
| `actor` | map | Human actor: `id` |
| `tenant` | map | Tenant context: `id` |
| `user` | map | User attributes: `role` |
| `risk` | map | Runtime risk: `score` |
| `resource` | map | Target resource: `tenant_id`, resource-specific fields |
| `request` | map | Request metadata: `channel`, `target_method`, `target_uri` |

All Java numeric types (`Integer`, `Short`, `Byte`, `BigInteger`, `Float`, `BigDecimal`) are automatically coerced into CEL's two native numeric types (`Long` or `Double`) before evaluation. See [Condition Evaluation — Evaluation Flow](condition-evaluation.md#evaluation-flow) for the full coercion table.

If a variable referenced in the condition is absent from `normalizedContext`, the result is `UNKNOWN` (not DENY).

## Expected

The expected block defines what the simulator should assert.

| Field | Type | Asserted | Description |
|-------|------|----------|-------------|
| `matcherDecision` | string | No | `MATCH` or `NO_MATCH` (informational) |
| `matchedRuleId` | string | Yes | Expected rule ID to match |
| `celDecision` | string | Yes | `GRANT` or `DENY` (case-insensitive) |
| `finalDecision` | string | Yes | `GRANT`, `DENY`, or `NO_MATCH` (case-insensitive) |
| `failedConditions` | array | No | List of failing CEL sub-expressions (informational) |
| `resolvedResources` | array | No | Expected resolved resources (reserved) |
| `targetClientId` | string | No | Expected target client (reserved) |
| `authorityDecision` | string | No | Expected authority decision (reserved) |

Fields marked "No" under "Asserted" are present in the schema for documentation purposes but are not currently checked by the simulator.

## Example

```json
{
  "name": "grant-invoice-read",
  "description": "Agent request with correct tenant, role, trust, and channel should be granted",
  "request": {
    "gatewayMethod": "POST",
    "gatewayPath": "/gateway/api/v1/access/check-permission",
    "headers": {
      "Keymate-Client-Id": "invoice-agent",
      "Keymate-Target-Client-Id": "invoice-api",
      "Keymate-Target-Method": "GET",
      "Keymate-Target-URI": "/api/v1/tenants/T-001/invoices/INV-123"
    }
  },
  "tokenClaims": {
    "sub": "invoice-agent",
    "azp": "invoice-agent",
    "agent_type": "mcp-client",
    "trust_level": 3,
    "actor_id": "user-789",
    "tenant_id": "T-001"
  },
  "normalizedContext": {
    "agent":    { "id": "invoice-agent", "type": "mcp-client", "trust_level": 3 },
    "actor":    { "id": "user-789" },
    "tenant":   { "id": "T-001" },
    "user":     { "role": "finance-manager" },
    "risk":     { "score": 4 },
    "resource": { "tenant_id": "T-001", "invoice_id": "INV-123" },
    "request":  { "target_method": "GET", "target_uri": "/api/v1/tenants/T-001/invoices/INV-123", "channel": "AGENT" }
  },
  "expected": {
    "matcherDecision": "MATCH",
    "matchedRuleId": "agent.invoice.read",
    "celDecision": "GRANT",
    "finalDecision": "GRANT"
  }
}
```

## Deny Scenario Example

```json
{
  "name": "deny-low-trust",
  "description": "Matching rule should deny when trust level is below threshold",
  "request": {
    "gatewayMethod": "POST",
    "gatewayPath": "/gateway/api/v1/access/check-permission",
    "headers": {
      "Keymate-Client-Id": "invoice-agent",
      "Keymate-Target-Client-Id": "invoice-api",
      "Keymate-Target-Method": "GET",
      "Keymate-Target-URI": "/api/v1/tenants/T-001/invoices/INV-456"
    }
  },
  "normalizedContext": {
    "agent":    { "id": "invoice-agent", "type": "mcp-client", "trust_level": 2 },
    "actor":    { "id": "user-789" },
    "tenant":   { "id": "T-001" },
    "user":     { "role": "finance-manager" },
    "risk":     { "score": 4 },
    "resource": { "tenant_id": "T-001", "invoice_id": "INV-456" },
    "request":  { "target_method": "GET", "target_uri": "/api/v1/tenants/T-001/invoices/INV-456", "channel": "AGENT" }
  },
  "expected": {
    "matcherDecision": "MATCH",
    "matchedRuleId": "agent.invoice.read",
    "celDecision": "DENY",
    "finalDecision": "DENY",
    "failedConditions": ["agent.trust_level >= 3"]
  }
}
```
