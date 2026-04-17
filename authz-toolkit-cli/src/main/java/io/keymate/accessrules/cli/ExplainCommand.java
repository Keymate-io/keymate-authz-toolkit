package io.keymate.accessrules.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.keymate.accessrules.cel.CelCompilerProvider;
import io.keymate.accessrules.cel.CelPolicyEvaluator;
import io.keymate.accessrules.cel.ConditionTrace;
import io.keymate.accessrules.cel.EvaluationResult;
import io.keymate.accessrules.loader.PolicyLoader;
import io.keymate.accessrules.matching.MatchRequest;
import io.keymate.accessrules.matching.MatchResult;
import io.keymate.accessrules.matching.RuleMatcher;
import io.keymate.accessrules.model.AccessRuleSet;
import io.keymate.accessrules.model.Rule;
import io.keymate.accessrules.simulation.Scenario;
import io.keymate.accessrules.simulation.ScenarioFile;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(name = "explain", description = "Explain why a scenario was granted or denied, with per-condition trace")
public class ExplainCommand implements Callable<Integer> {

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
            RuleMatcher matcher = new RuleMatcher(ruleSet);

            for (Scenario scenario : sf.scenarios()) {
                out.println("  ── " + scenario.name() + " ──");
                explainScenario(out, matcher, evaluator, scenario);
                out.println();
            }
            return 0;
        } catch (Exception e) {
            out.println("  ✗ Error — " + e.getMessage());
            return 1;
        }
    }

    private void explainScenario(PrintWriter out, RuleMatcher matcher,
                                  CelPolicyEvaluator evaluator, Scenario scenario) {

        Scenario.Request req = scenario.request();
        MatchRequest matchRequest = new MatchRequest(
                req.sourceClientId(),
                req.targetClientId(),
                req.targetMethod(),
                req.targetUri()
        );

        MatchResult matchResult = matcher.match(matchRequest);

        if (!matchResult.matched()) {
            out.println("  Match:    ✗ No rule matched");
            out.println("  Reason:   " + matchResult.failureReason());
            out.println("  Decision: NO_MATCH");
            return;
        }

        Rule rule = matchResult.rule();
        out.println("  Match:    ✓ " + rule.id());

        if (rule.match() != null && rule.match().targetClientId() != null) {
            out.println("  Target:   " + rule.match().targetClientId());
        }

        if (rule.condition() != null && !rule.condition().isBlank()) {
            Map<String, Object> context = scenario.normalizedContext() != null
                    ? scenario.normalizedContext()
                    : Collections.emptyMap();
            EvaluationResult result = evaluator.evaluateWithTrace(rule.condition().trim(), context);

            if (result.trace() != null) {
                for (ConditionTrace ct : result.trace()) {
                    String icon = "true".equals(ct.result()) ? "✓" : "✗";
                    out.println("  " + icon + " " + ct.condition());
                }
            }

            out.println("  Decision: " + result.decision());
        }
    }
}
