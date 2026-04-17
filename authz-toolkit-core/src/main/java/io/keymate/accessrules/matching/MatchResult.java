package io.keymate.accessrules.matching;

import io.keymate.accessrules.model.Rule;

public record MatchResult(
        boolean matched,
        Rule rule,
        String failureReason
) {

    public static MatchResult matched(Rule rule) {
        return new MatchResult(true, rule, null);
    }

    public static MatchResult noMatch(String reason) {
        return new MatchResult(false, null, reason);
    }
}
