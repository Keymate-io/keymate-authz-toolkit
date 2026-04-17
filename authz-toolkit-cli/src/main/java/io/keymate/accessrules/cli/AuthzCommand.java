package io.keymate.accessrules.cli;

import picocli.CommandLine.Command;

@Command(
    name = "authz",
    description = "Authorization policy commands — validate, simulate, explain",
    mixinStandardHelpOptions = true,
    subcommands = {
        ValidateCommand.class,
        SimulateCommand.class,
        ExplainCommand.class
    }
)
public class AuthzCommand implements Runnable {

    @Override
    public void run() {
        picocli.CommandLine.usage(this, System.out);
    }
}
