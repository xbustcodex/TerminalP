package com.termux.app.privilege;

import androidx.annotation.NonNull;

import java.util.Map;

/**
 * Runs a pinned argv with a controlled environment, enforcing a timeout and an output bound.
 *
 * <p>This abstraction exists so that unit tests can substitute a fake runner and assert the
 * auth/allowlist/argument-construction logic without actually asking for root on the test
 * host. The only production implementation is {@link OsProcessRunner}.</p>
 */
public interface PrivilegedProcessRunner {

    /**
     * @param executable  Absolute path of the executable to run (TerminalP-derived, allowlisted).
     * @param args        Fixed argument vector (TerminalP-derived only).
     * @param environment Extra environment variables (TerminalP bootstrap environment).
     * @param timeoutMillis Hard timeout after which the process is destroyed.
     * @param maxOutputBytes Hard cap on captured stdout.
     */
    @NonNull
    ProcessRunResult run(@NonNull String executable, @NonNull String[] args,
                         @NonNull Map<String, String> environment,
                         long timeoutMillis, int maxOutputBytes);
}
