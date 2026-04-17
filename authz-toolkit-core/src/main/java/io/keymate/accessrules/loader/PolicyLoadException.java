package io.keymate.accessrules.loader;

/**
 * Checked exception thrown when a policy YAML file cannot be loaded or parsed.
 */
public class PolicyLoadException extends Exception {

    public PolicyLoadException(String message) {
        super(message);
    }

    public PolicyLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
