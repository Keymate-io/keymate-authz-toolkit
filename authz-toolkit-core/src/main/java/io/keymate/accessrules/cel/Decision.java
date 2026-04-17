package io.keymate.accessrules.cel;

/**
 * The outcome of a policy evaluation.
 */
public enum Decision {

    /** Access is granted — the CEL expression evaluated to true. */
    GRANT,

    /** Access is denied — the CEL expression evaluated to false. */
    DENY,

    /** The result could not be determined (e.g. missing context variable). */
    UNKNOWN,

    /** An error occurred during evaluation. */
    ERROR
}
