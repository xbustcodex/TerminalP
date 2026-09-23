package com.termux.app.privilege;

import androidx.annotation.NonNull;

/** Result of running a pinned privileged operation's argv. */
public final class ProcessRunResult {

    public final boolean started;
    public final boolean timedOut;
    public final int exitCode;
    @NonNull
    public final String stdout;
    @NonNull
    public final String error;

    private ProcessRunResult(final boolean started, final boolean timedOut, final int exitCode,
                             @NonNull final String stdout, @NonNull final String error) {
        this.started = started;
        this.timedOut = timedOut;
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.error = error;
    }

    public static ProcessRunResult success(final int exitCode, @NonNull final String stdout) {
        return new ProcessRunResult(true, false, exitCode, stdout, "");
    }

    public static ProcessRunResult failedToStart(@NonNull final String error) {
        return new ProcessRunResult(false, false, -1, "", error);
    }

    public static ProcessRunResult timedOut() {
        return new ProcessRunResult(true, true, -1, "", "timed out");
    }
}
