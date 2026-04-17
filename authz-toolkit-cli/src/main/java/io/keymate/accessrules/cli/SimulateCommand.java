package io.keymate.accessrules.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.keymate.accessrules.cel.CelCompilerProvider;
import io.keymate.accessrules.cel.CelPolicyEvaluator;
import io.keymate.accessrules.loader.PolicyLoader;
import io.keymate.accessrules.model.AccessRuleSet;
import io.keymate.accessrules.simulation.ScenarioFile;
import io.keymate.accessrules.simulation.ScenarioResult;
import io.keymate.accessrules.simulation.SimulationReport;
import io.keymate.accessrules.simulation.Simulator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "simulate", description = "Run scenarios against a policy and report results")
public class SimulateCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to policy YAML file")
    private Path policyFile;

    @Option(names = {"--scenario", "-s"}, required = true, description = "Path to scenario JSON file")
    private Path scenarioFile;

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        if (!Files.exists(policyFile)) {
            out.println("  ✗ File not found — " + policyFile);
            return 1;
        }
        if (!Files.exists(scenarioFile)) {
            out.println("  ✗ File not found — " + scenarioFile);
            return 1;
        }
        try {
            AccessRuleSet ruleSet = PolicyLoader.load(policyFile);
            ObjectMapper json = new ObjectMapper();
            ScenarioFile sf = json.readValue(scenarioFile.toFile(), ScenarioFile.class);

            CelCompilerProvider compiler = new CelCompilerProvider();
            CelPolicyEvaluator evaluator = new CelPolicyEvaluator(compiler);
            Simulator simulator = new Simulator(evaluator::evaluate);

            SimulationReport report = simulator.run(ruleSet, sf.scenarios());

            for (ScenarioResult r : report.results()) {
                String icon = r.passed() ? "✓" : "✗";
                String decision = r.finalDecision() != null ? r.finalDecision() : "NO_MATCH";
                out.println("  " + icon + " " + r.scenarioName() + " → " + decision);
                if (!r.passed() && r.failureDetail() != null) {
                    out.println("    reason: " + r.failureDetail());
                }
            }

            out.println();
            out.println("  " + report.passed() + "/" + report.totalScenarios() + " passed");

            return report.failed() > 0 ? 1 : 0;
        } catch (Exception e) {
            out.println("  ✗ Error — " + e.getMessage());
            return 1;
        }
    }
}
