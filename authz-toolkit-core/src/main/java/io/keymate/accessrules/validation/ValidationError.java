package io.keymate.accessrules.validation;

/**
 * A single validation finding for a policy.
 *
 * @param severity ERROR or WARNING
 * @param path     location in the policy tree (e.g. "rules[0].match.http.methods")
 * @param message  human-readable description
 */
public record ValidationError(
        Severity severity,
        String path,
        String message
) {

    public enum Severity { ERROR, WARNING }

    public static ValidationError error(String path, String message) {
        return new ValidationError(Severity.ERROR, path, message);
    }

    public static ValidationError warning(String path, String message) {
        return new ValidationError(Severity.WARNING, path, message);
    }
}
