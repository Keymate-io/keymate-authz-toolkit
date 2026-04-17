package io.keymate.accessrules.matching;

public record MatchRequest(
        String sourceClientId,
        String targetClientId,
        String method,
        String targetUri
) {
}
