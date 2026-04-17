package io.keymate.accessrules.matching;

import io.keymate.accessrules.model.AccessRuleSet;
import io.keymate.accessrules.model.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class RuleMatcher {

    private static final Logger log = LoggerFactory.getLogger(RuleMatcher.class);

    private final AccessRuleSet ruleSet;
    private final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();

    public RuleMatcher(AccessRuleSet ruleSet) {
        this.ruleSet = ruleSet;
        if (ruleSet != null && ruleSet.rules() != null) {
            precompilePatterns(ruleSet.rules());
        }
    }

    public RuleMatcher() {
        this.ruleSet = null;
    }

    private void precompilePatterns(List<Rule> rules) {
        for (Rule rule : rules) {
            if (rule.targetUriPatterns() == null) {
                continue;
            }
            for (String pattern : rule.targetUriPatterns()) {
                patternCache.computeIfAbsent(pattern, RuleMatcher::compilePattern);
            }
        }
    }

    private static Pattern compilePattern(String pattern) {
        return Pattern.compile("^" + pattern.replace("*", "[^/]+") + "$");
    }

    public MatchResult match(MatchRequest request) {
        if (ruleSet == null) {
            return MatchResult.noMatch("No rule set bound to this matcher");
        }
        return match(request, ruleSet.rules());
    }

    public MatchResult match(MatchRequest request, List<Rule> rules) {
        if (rules == null || rules.isEmpty()) {
            return MatchResult.noMatch("No rules defined in rule set");
        }

        for (Rule rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }

            // 1. Check sourceClientId
            if (rule.match() != null && rule.match().sourceClientId() != null) {
                if (!rule.match().sourceClientId().equals(request.sourceClientId())) {
                    continue;
                }
            }

            // 2. Check targetClientId
            if (rule.match() != null && rule.match().targetClientId() != null) {
                if (!rule.match().targetClientId().equals(request.targetClientId())) {
                    continue;
                }
            }

            // 3. Check HTTP method
            if (rule.methods() != null && !rule.methods().isEmpty()) {
                if (!rule.methods().contains(request.method())) {
                    continue;
                }
            }

            // 4. Check target URI patterns
            if (rule.targetUriPatterns() != null && !rule.targetUriPatterns().isEmpty()) {
                if (!matchesAnyPattern(request.targetUri(), rule.targetUriPatterns())) {
                    continue;
                }
            }

            log.debug("Rule matched: {}", rule.id());
            return MatchResult.matched(rule);
        }

        log.debug("No rule matched for sourceClientId={}, method={}, uri={}",
                request.sourceClientId(), request.method(), request.targetUri());
        return MatchResult.noMatch("No rule matched the request");
    }

    private boolean matchesAnyPattern(String uri, List<String> patterns) {
        for (String pattern : patterns) {
            if (matchesWildcardPattern(uri, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesWildcardPattern(String uri, String pattern) {
        Pattern compiled = patternCache.computeIfAbsent(pattern, RuleMatcher::compilePattern);
        return compiled.matcher(uri).matches();
    }
}
