# Keymate AuthZ Toolkit

The open-source toolkit for fine-grained authorization. The repository ships three surfaces: the **Access Rules** declarative policy DSL, the **Access Rule Engine** (embeddable evaluation core), and **kmctl** (the command-line binary for authoring, validating, simulating, and explaining policies).

**Access Rules** are declarative policies that decide *who* can do *what*, under *which conditions*, on behalf of *which actor*.

It is built for a world where a single "authenticated request" is no longer enough context to make an authorization decision. Agent identity, human actor, tenant, trust level, channel, and runtime risk must all be weighed together — and the decision must be explainable.

---

## ✨ Key Features

- **Declarative policies**
  Policies are YAML documents (`AccessRuleSet`) with a routing-level matcher and an optional fine-grained `condition`.

- **Two-stage evaluation**
  A matcher selects the applicable rule by source client, target service, HTTP method, and URI pattern. A CEL expression then evaluates fine-grained business rules on top of the match.

- **Explainability as a first-class feature**
  `kmctl authz explain` produces a per-condition trace for both GRANT and DENY outcomes, so a reviewer can see exactly which clause passed or failed.

- **Scenario-driven testing**
  Policies ship with JSON scenarios that assert the expected decision. `kmctl authz simulate` runs them as a test suite against a policy.

- **Portable fat JAR**
  Single self-contained CLI binary with no external runtime dependencies beyond a JVM.

---

## 🧠 Design Principles

| Anti-pattern | This project |
|--------------|-------------|
| ❌ Binary allow/deny at gateway | ✅ Condition-level fine-grained checks |
| ❌ Opaque DENY decisions | ✅ Per-clause trace on every evaluation |
| ❌ Policy drift between design and runtime | ✅ Same matcher + CEL logic in both |
| ❌ Hardcoded authorization in services | ✅ Declarative `AccessRuleSet` artifacts |

The guiding philosophy:

> **An authorization decision you cannot explain is one you cannot audit.**

---

## 📦 Technology Stack

- Java 17+
- [CEL-Java](https://github.com/google/cel-java) — Common Expression Language runtime
- Jackson (YAML + JSON)
- picocli
- SLF4J

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+

### Build

```bash
mvn clean package
```

This runs the full test suite and produces the CLI fat JAR under:

```
authz-toolkit-cli/target/keymate-authz-toolkit-cli-<version>.jar
```

### Create an Alias

```bash
alias kmctl="java -jar $(ls authz-toolkit-cli/target/keymate-authz-toolkit-cli-*.jar | head -n 1)"
```

Verify:

```bash
kmctl --help
```

---

## ▶️ Commands

### `kmctl authz validate`

Validates the structure of a policy YAML and compiles its CEL expressions.

```bash
kmctl authz validate my-policy.yaml
```

### `kmctl authz simulate`

Runs all scenarios in a JSON file against a policy and reports pass/fail per scenario.

```bash
kmctl authz simulate my-policy.yaml -s my-scenarios.json
```

### `kmctl authz explain`

For each scenario, traces which CEL sub-conditions passed or failed.

```bash
kmctl authz explain my-policy.yaml -s my-scenarios.json
```

See [docs/getting-started.md](docs/getting-started.md) for a full walkthrough.

---

## 🧩 Modules

This is a multi-module Maven project.

| Module | Purpose |
|--------|---------|
| [`authz-toolkit-core`](authz-toolkit-core/README.md) | Embeddable engine library — policy loading, rule matching, CEL evaluation, validation, simulation. |
| [`authz-toolkit-cli`](authz-toolkit-cli/README.md) | `kmctl` command-line binary — `authz validate / simulate / explain` commands built on top of the core. |

---

## 📚 Documentation

- [Getting Started](docs/getting-started.md) — build, alias, first policy end-to-end.
- [Policy Format](docs/policy-format.md) — `AccessRuleSet` specification.
- [Scenario Format](docs/scenario-format.md) — writing test scenarios.
- [CLI Reference](docs/cli-reference.md) — all commands, options, exit codes.
- [Rule Matching](docs/matching.md) — how the matcher resolves a request to a rule.
- [Condition Evaluation](docs/condition-evaluation.md) — CEL expressions, context variables, trace behavior.

---

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.

---

## 🔒 Security

To report a vulnerability, please follow [SECURITY.md](SECURITY.md). Do **not** open public issues for security disclosures.

---

## 📄 License

This project is licensed under the **Apache License 2.0**. See [LICENSE](LICENSE) and [NOTICE](NOTICE) for details.

---

## ⚠️ Disclaimer

This tool is provided as-is. Always simulate and review your policies in a non-production environment before applying them to real traffic.
