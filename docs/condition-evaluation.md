# Condition Evaluation

The `condition` field in a policy rule is a [CEL (Common Expression Language)](https://cel.dev) boolean expression. CEL is developed by Google and actively used in Kubernetes.

## Context Variables

Seven context variables are available in condition expressions. Each is a `map(string, dyn)`:

| Variable | Purpose | Common fields |
|----------|---------|---------------|
| `agent` | Calling agent properties | `id`, `type`, `trust_level` |
| `actor` | Human actor on whose behalf the agent acts | `id` |
| `tenant` | Tenant context | `id` |
| `user` | User attributes | `role` |
| `risk` | Runtime risk signal | `score` |
| `resource` | Target resource properties | `tenant_id`, resource-specific fields |
| `request` | Request metadata | `channel`, `target_method`, `target_uri` |

## Expression Syntax

Conditions are standard CEL expressions that must evaluate to a boolean. Multiple conditions are joined with `&&`:

```cel
agent.type == 'mcp-client'
&& agent.trust_level >= 3
&& actor.id != ''
&& request.channel == 'AGENT'
&& tenant.id == resource.tenant_id
&& user.role == 'finance-manager'
&& risk.score <= 5
```

### Supported Operators

| Operator | Example |
|----------|---------|
| Equality | `agent.type == 'mcp-client'` |
| Inequality | `actor.id != ''` |
| Comparison | `agent.trust_level >= 3`, `risk.score <= 5` |
| Logical AND | `&&` |
| Cross-field | `tenant.id == resource.tenant_id` |

## Evaluation Flow

1. **Numeric coercion:** Java numeric types in the context are recursively normalized to CEL's two native numeric types — `Long` (CEL `int`) and `Double` (CEL `double`):

   | Java type | Coerced to |
   |-----------|-----------|
   | `Integer`, `Short`, `Byte`, `BigInteger` | `Long` |
   | `Long` | unchanged |
   | `Float` | `Double` |
   | `BigDecimal` (whole number) | `Long` |
   | `BigDecimal` (fractional) | `Double` |
   | `Double` | unchanged |

   Coercion is applied recursively through nested maps and lists. `String`, `Boolean`, and unrecognized types pass through untouched.

2. **Missing variable detection:** Before compilation, the expression is scanned for references to the 7 declared variables. If a referenced variable is missing from the context, the result is `UNKNOWN` (not DENY).

3. **Compilation and evaluation:** The expression is compiled and evaluated against the provided context.

4. **Decision mapping:**
   - `true` -> `GRANT`
   - `false` -> `DENY`
   - Non-boolean result -> `UNKNOWN`
   - Exception -> `ERROR`

## Trace Behavior

The `explain` command uses `evaluateWithTrace()` which provides per-condition breakdown:

- **On GRANT and DENY:** The expression is split on `&&`, and each sub-condition is evaluated independently. Each produces a `true`, `false`, `unknown`, or `error` result. Both decisions carry a full trace so a granted request can also be explained clause by clause.
- **On UNKNOWN or ERROR:** No per-condition trace is produced. The full-expression result is returned as-is, since the evaluation could not complete deterministically.

All sub-conditions are evaluated regardless of earlier failures, so the trace is not short-circuited.

### Trace Example (DENY)

For the expression above with `agent.trust_level = 2`:

```
  ✓ agent.type == 'mcp-client'
  ✗ agent.trust_level >= 3
  ✓ actor.id != ''
  ✓ request.channel == 'AGENT'
  ✓ tenant.id == resource.tenant_id
  ✓ user.role == 'finance-manager'
  ✓ risk.score <= 5
  Decision: DENY
```

6 out of 7 conditions passed, but `agent.trust_level >= 3` failed. The decision is DENY.

### Trace Example (GRANT)

For the same expression with all fields satisfied:

```
  ✓ agent.type == 'mcp-client'
  ✓ agent.trust_level >= 3
  ✓ actor.id != ''
  ✓ request.channel == 'AGENT'
  ✓ tenant.id == resource.tenant_id
  ✓ user.role == 'finance-manager'
  ✓ risk.score <= 5
  Decision: GRANT
```

## Limitations

- **AND-only splitting:** Trace splits on `&&` only. Expressions using `||`, nested parentheses, or complex logical structures will not be split into individual traces.
- **Map-based context:** All context variables are `map(string, dyn)`. Nested access (e.g. `agent.trust_level`) uses CEL map key access semantics.
- **No short-circuit in trace:** When tracing, all sub-conditions are evaluated regardless of earlier failures. This provides a complete picture of which conditions pass and which fail — on both GRANT and DENY.

## Matching vs Condition

Rule matching and condition evaluation are separate stages:

| Stage | Purpose | When |
|-------|---------|------|
| **Matcher** | Route-level: which rule applies? | Always runs first |
| **Condition** | Fine-grained: should this be allowed? | Runs only if a rule matches and has a `condition` |

If a rule matches but has no `condition`, the result is `GRANT` (no fine-grained check needed). If a rule matches and the condition evaluates to `false`, the result is `DENY` without proceeding to authority checks.
