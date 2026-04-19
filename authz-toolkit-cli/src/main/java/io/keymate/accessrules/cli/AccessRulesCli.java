package io.keymate.accessrules.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "kmctl",
    description = "Keymate CLI",
    mixinStandardHelpOptions = true,
    version = "kmctl 0.1.0",
    subcommands = {
        AuthzCommand.class
    }
)
public class AccessRulesCli implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new AccessRulesCli()).execute(args);
        System.exit(exitCode);
    }
}
