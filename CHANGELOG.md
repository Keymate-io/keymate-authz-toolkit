# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-04-17
### Added
- Initial public release of Keymate AuthZ Toolkit (kmctl + Access Rules DSL + Access Rule Engine).
- `AccessRuleSet` policy schema: rule matching (sourceClientId, targetClientId, HTTP methods, URI patterns) plus optional CEL `condition` for fine-grained evaluation.
- `kmctl authz validate` — structural and CEL syntax validation of policy YAML.
- `kmctl authz simulate` — runs scenarios against a policy and reports pass/fail.
- `kmctl authz explain` — per-condition trace for both GRANT and DENY outcomes.
- Numeric coercion across `Integer`, `Short`, `Byte`, `BigInteger`, `Float`, `BigDecimal` into CEL-native `Long` / `Double`.
- Thread-safe pattern caching in the rule matcher.
