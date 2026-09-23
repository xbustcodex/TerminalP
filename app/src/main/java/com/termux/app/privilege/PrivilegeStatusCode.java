package com.termux.app.privilege;

/**
 * Structured status codes returned by the privileged execution bridge.
 *
 * <p>These are the canonical error values TerminalP returns to the caller (PrimeTech
 * Terminal). {@link #STATUS_OK} indicates a completed (possibly non-zero exit) execution;
 * every other value is a structured failure that must NOT fall back to direct root on the
 * caller side. Callers must map these to their own error model (e.g. TERMINALP_UNAVAILABLE,
 * CALLER_NOT_AUTHORIZED, OPERATION_NOT_SUPPORTED, TIMEOUT, EXECUTION_FAILED).</p>
 */
public final class PrivilegeStatusCode {

    private PrivilegeStatusCode() {
    }

    /** The request was authenticated and executed. stdout/exitCode are meaningful. */
    public static final int STATUS_OK = 0;

    /** No caller was resolvable on this binder thread. */
    public static final int STATUS_CALLER_UNAVAILABLE = 1;

    /** The calling package/signature is not in the TerminalP allowlist. */
    public static final int STATUS_CALLER_NOT_AUTHORIZED = 2;

    /** The requested operation identifier is not in the TerminalP-owned registry. */
    public static final int STATUS_OPERATION_NOT_SUPPORTED = 3;

    /** The operation is supported but not runnable on this device/ABI. */
    public static final int STATUS_OPERATION_UNAVAILABLE = 4;

    /** The approved executable failed pre-execution validation. */
    public static final int STATUS_EXECUTION_INVALID = 5;

    /** The approved executable could not be started. */
    public static final int STATUS_EXECUTION_FAILED = 6;

    /** The operation exceeded its timeout budget. */
    public static final int STATUS_TIMEOUT = 7;
}
