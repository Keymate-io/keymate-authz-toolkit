# Rule Matching

The matcher determines which rule applies to an incoming request. It runs before condition evaluation.

## Match Request

A match request contains four fields extracted from gateway headers:

| Field | Header | Description |
|-------|--------|-------------|
| `sourceClientId` | `Keymate-Client-Id` | The originating client |
| `targetClientId` | `Keymate-Target-Client-Id` | The destination service |
| `method` | `Keymate-Target-Method` | HTTP method on target |
| `targetUri` | `Keymate-Target-URI` | URI on target resource |

## Matching Order

Rules are evaluated in declaration order. The **first** matching rule wins. For each rule:

1. **Skip if disabled** — `enabled: false` skips the rule entirely.
2. **sourceClientId** — If the rule specifies `match.sourceClientId`, it must exactly match the request. If omitted in the rule, any source matches.
3. **targetClientId** — If the rule specifies `match.targetClientId`, it must exactly match the request. If omitted in the rule, any target matches.
4. **HTTP method** — The request method must be in the rule's `methods` list. Case-sensitive exact match.
5. **URI pattern** — The request URI must match at least one pattern in `targetUriPatterns`.

All checks must pass for a rule to match. The first rule passing all checks is returned.

## URI Wildcards

The `*` wildcard matches any single path segment (characters between `/` boundaries).

| Pattern | Matches | Does Not Match |
|---------|---------|----------------|
| `/api/v1/tenants/*/invoices/*` | `/api/v1/tenants/T-001/invoices/INV-123` | `/api/v1/tenants/T-001/sub/invoices/INV-123` |
| `/api/*/users` | `/api/v2/users` | `/api/v2/admin/users` |
| `/files/*` | `/files/report.pdf` | `/files/2024/report.pdf` |

Internally, `*` is converted to the regex `[^/]+` and the pattern is anchored with `^` and `$`.

## Match Result

| Outcome | Description |
|---------|-------------|
| **Matched** | A rule was found. Condition evaluation proceeds. |
| **No match** | No rule applies. The request is unhandled by this policy. |

When no rule matches, the result includes a failure reason string (e.g. "No rule matched the request").

## Decision Flow After Matching

```
Request arrives
    │
    ▼
Matcher: find first matching rule
    │
    ├─ No match → NO_MATCH (request not covered by policy)
    │
    ├─ Match + no condition → GRANT
    │
    └─ Match + condition
           │
           ├─ condition = true  → GRANT
           └─ condition = false → DENY
```
