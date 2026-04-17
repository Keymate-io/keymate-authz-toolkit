package io.keymate.accessrules.validation;

import io.keymate.accessrules.cel.CelCompilerProvider;
import io.keymate.accessrules.model.AccessRuleSet;
import io.keymate.accessrules.model.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class PolicyValidator {

    private static final Logger log = LoggerFactory.getLogger(PolicyValidator.class);

    private final CelCompilerProvider celCompiler;

    public PolicyValidator(CelCompilerProvider celCompiler) {
        this.celCompiler = celCompiler;
    }

    public ValidationReport validate(AccessRuleSet ruleSet) {
        log.debug("Validating policy '{}'", ruleSet.name());
        List<ValidationError> findings = new ArrayList<>();

        validateTopLevel(ruleSet, findings);
        validateRules(ruleSet, findings);

        ValidationReport report = new ValidationReport(List.copyOf(findings));
        if (report.isValid()) {
            log.debug("Policy '{}' is valid ({} warning(s))", ruleSet.name(), report.warningCount());
        } else {
            log.debug("Policy '{}' has {} error(s)", ruleSet.name(), report.errorCount());
        }
        return report;
    }

    private void validateTopLevel(AccessRuleSet ruleSet, List<ValidationError> findings) {
        if (isBlank(ruleSet.name())) {
            findings.add(ValidationError.error("name", "name is required"));
        }
        if (isBlank(ruleSet.kind())) {
            findings.add(ValidationError.warning("kind", "kind is recommended"));
        }
        if (ruleSet.version() == null || ruleSet.version() < 1) {
            findings.add(ValidationError.error("version", "version is required and must be >= 1"));
        }
    }

    private void validateRules(AccessRuleSet ruleSet, List<ValidationError> findings) {
        if (ruleSet.rules() == null || ruleSet.rules().isEmpty()) {
            findings.add(ValidationError.error("rules", "at least one rule is required"));
            return;
        }

        Set<String> ruleIds = new HashSet<>();

        for (int i = 0; i < ruleSet.rules().size(); i++) {
            Rule rule = ruleSet.rules().get(i);
            String prefix = "rules[" + i + "]";
            validateRule(rule, prefix, ruleIds, findings);
        }
    }

    private void validateRule(Rule rule, String prefix, Set<String> ruleIds,
                              List<ValidationError> findings) {
        if (isBlank(rule.id())) {
            findings.add(ValidationError.error(prefix + ".id", "rule id is required"));
        } else if (!ruleIds.add(rule.id())) {
            findings.add(ValidationError.error(prefix + ".id",
                    "duplicate rule id: '" + rule.id() + "'"));
        }

        if (rule.methods() == null || rule.methods().isEmpty()) {
            findings.add(ValidationError.error(prefix + ".methods",
                    "at least one HTTP method is required"));
        }

        if (rule.targetUriPatterns() == null || rule.targetUriPatterns().isEmpty()) {
            findings.add(ValidationError.error(prefix + ".targetUriPatterns",
                    "at least one target URI pattern is required"));
        }

        if (rule.match() != null && isBlank(rule.match().sourceClientId())) {
            findings.add(ValidationError.warning(prefix + ".match.sourceClientId",
                    "sourceClientId is recommended"));
        }

        if (rule.condition() != null && !rule.condition().isBlank()) {
            List<String> celErrors = celCompiler.validate(rule.condition().trim());
            for (String err : celErrors) {
                findings.add(ValidationError.error(prefix + ".condition", err));
            }
        }

        if (isBlank(rule.intent())) {
            findings.add(ValidationError.warning(prefix + ".intent",
                    "intent is recommended (ALLOW/DENY)"));
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
