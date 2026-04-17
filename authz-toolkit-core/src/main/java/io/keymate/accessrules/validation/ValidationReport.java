package io.keymate.accessrules.validation;

import java.util.List;

/**
 * Aggregated result of policy validation.
 *
 * @param findings all validation errors and warnings
 */
public record ValidationReport(List<ValidationError> findings) {

    public boolean isValid() {
        return findings.stream()
                .noneMatch(f -> f.severity() == ValidationError.Severity.ERROR);
    }

    public List<ValidationError> errors() {
        return findings.stream()
                .filter(f -> f.severity() == ValidationError.Severity.ERROR)
                .toList();
    }

    public List<ValidationError> warnings() {
        return findings.stream()
                .filter(f -> f.severity() == ValidationError.Severity.WARNING)
                .toList();
    }

    public int errorCount() {
        return (int) findings.stream()
                .filter(f -> f.severity() == ValidationError.Severity.ERROR)
                .count();
    }

    public int warningCount() {
        return (int) findings.stream()
                .filter(f -> f.severity() == ValidationError.Severity.WARNING)
                .count();
    }
}
