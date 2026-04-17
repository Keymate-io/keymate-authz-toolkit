package io.keymate.accessrules.simulation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Scenario(
        String name,
        String description,
        Request request,
        Map<String, Object> tokenClaims,
        Map<String, Object> normalizedContext,
        Expected expected
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Request(
            String gatewayMethod,
            String gatewayPath,
            Map<String, String> headers,
            Map<String, Object> body
    ) {
        public String sourceClientId() {
            return headers != null ? headers.get("Keymate-Client-Id") : null;
        }

        public String targetClientId() {
            return headers != null ? headers.get("Keymate-Target-Client-Id") : null;
        }

        public String targetMethod() {
            return headers != null ? headers.get("Keymate-Target-Method") : null;
        }

        public String targetUri() {
            return headers != null ? headers.get("Keymate-Target-URI") : null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Expected(
            String matcherDecision,
            String matchedRuleId,
            List<ResolvedResource> resolvedResources,
            String targetClientId,
            String celDecision,
            String authorityDecision,
            String finalDecision,
            List<String> failedConditions
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResolvedResource(
            String name,
            List<String> scopes
    ) {}
}
