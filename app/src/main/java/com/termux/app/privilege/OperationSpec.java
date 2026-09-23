package com.termux.app.privilege;

import androidx.annotation.NonNull;

/**
 * A single approved privileged operation in the TerminalP-owned registry.
 *
 * <p>Every {@link OperationSpec} is created only by {@link OperationRegistry} from
 * compile-time pinned values. The caller (PrimeTech Terminal) never supplies the
 * executable, its arguments, or an arbitrary shell string; it only supplies an
 * {@code operationId}. This keeps the allowlist authoritative and prevents callers from
 * turning the bridge into a generic root shell.</p>
 */
public final class OperationSpec {

    /** Identifier the caller requests. Not caller-visible as an executable path. */
    @NonNull
    public final String operationId;

    /** The root/spawn binary TerminalP will invoke (e.g. {@code $PREFIX/bin/sudo}). */
    @NonNull
    public final String execPath;

    /**
     * The exact, fixed argument vector passed to {@link #execPath}. May be empty.
     * Contains only TerminalP-derived values (e.g. the absolute path of a TerminalP-owned
     * executable). Never contains caller-supplied text.
     */
    @NonNull
    public final String[] args;

    /** Runs the process with the elevated TerminalP environment (root via sudo). */
    public final boolean requiresRoot;

    /** Hard timeout, in milliseconds, after which the process is destroyed. */
    public final long timeoutMillis;

    /** Hard cap on captured stdout bytes. Excess output is truncated. */
    public final int maxOutputBytes;

    OperationSpec(@NonNull final String operationId, @NonNull final String execPath,
                  @NonNull final String[] args, final boolean requiresRoot,
                  final long timeoutMillis, final int maxOutputBytes) {
        this.operationId = operationId;
        this.execPath = execPath;
        this.args = args;
        this.requiresRoot = requiresRoot;
        this.timeoutMillis = timeoutMillis;
        this.maxOutputBytes = maxOutputBytes;
    }
}
