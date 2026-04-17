package io.keymate.accessrules.cli;

import io.keymate.accessrules.cel.CelCompilerProvider;
import io.keymate.accessrules.loader.PolicyLoader;
import io.keymate.accessrules.model.AccessRuleSet;
import io.keymate.accessrules.validation.PolicyValidator;
import io.keymate.accessrules.validation.ValidationError;
import io.keymate.accessrules.validation.ValidationReport;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "validate", description = "Validate Access Rules policy YAML and CEL syntax")
public class ValidateCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to policy YAML file")
    private Path policyFile;

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        if (!Files.exists(policyFile)) {
            out.println("  ✗ File not found — " + policyFile);
            return 1;
        }
        try {
            AccessRuleSet ruleSet = PolicyLoader.load(policyFile);
            CelCompilerProvider compiler = new CelCompilerProvider();
            PolicyValidator validator = new PolicyValidator(compiler);

            ValidationReport report = validator.validate(ruleSet);

            for (ValidationError finding : report.findings()) {
                String icon = finding.severity() == ValidationError.Severity.ERROR ? "✗" : "⚠";
                out.println("  " + icon + " [" + finding.path() + "] " + finding.message());
            }

            if (report.isValid()) {
                int ruleCount = ruleSet.rules() != null ? ruleSet.rules().size() : 0;
                String warnings = report.warningCount() > 0
                        ? ", " + report.warningCount() + " warning(s)"
                        : "";
                out.println("  ✓ Valid — " + ruleCount + " rule" + (ruleCount != 1 ? "s" : "") + warnings);
            }

            return report.isValid() ? 0 : 1;
        } catch (Exception e) {
            out.println("  ✗ Parse error — " + e.getMessage());
            return 1;
        }
    }
}
