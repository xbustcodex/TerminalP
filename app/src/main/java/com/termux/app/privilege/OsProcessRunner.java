package com.termux.app.privilege;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Production {@link PrivilegedProcessRunner} backed by {@link ProcessBuilder}.
 *
 * <p>For the {@code native_probe} operation this launches {@code $PREFIX/bin/sudo <probe>},
 * which elevates to root on a rooted device and runs the TerminalP-owned probe. The argv is
 * fully TerminalP-derived from the allowlist; no caller text is ever interpolated into the
 * command line. Output is read on a bounded stream and the process is force-destroyed on
 * timeout.</p>
 */
public final class OsProcessRunner implements PrivilegedProcessRunner {

    private static final int READ_BUFFER = 1024;
    private static final int MAX_LINES = 4096;

    @NonNull
    @Override
    public ProcessRunResult run(@NonNull final String executable, @NonNull final String[] args,
                                @NonNull final Map<String, String> environment,
                                final long timeoutMillis, final int maxOutputBytes) {
        final List<String> command = new ArrayList<>(args.length + 1);
        command.add(executable);
        for (final String arg : args) {
            command.add(arg);
        }

        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        builder.environment().putAll(environment);

        final Process process;
        try {
            process = builder.start();
        } catch (final IOException e) {
            return ProcessRunResult.failedToStart(e.getMessage());
        }

        final String stdout = readBounded(process, maxOutputBytes);

        boolean finished;
        try {
            finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return ProcessRunResult.timedOut();
        }

        if (!finished) {
            process.destroyForcibly();
            return ProcessRunResult.timedOut();
        }

        return ProcessRunResult.success(process.exitValue(), stdout);
    }

    private static String readBounded(@NonNull final Process process, final int maxOutputBytes) {
        final StringBuilder sb = new StringBuilder();
        final byte[] buffer = new byte[READ_BUFFER];
        long total = 0;
        int lines = 0;
        try (final InputStream in = process.getInputStream()) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > maxOutputBytes) {
                    final int room = Math.max(0, (int) (maxOutputBytes - (total - read)));
                    if (room > 0) {
                        sb.append(new String(buffer, 0, Math.min(room, read), java.nio.charset.StandardCharsets.UTF_8));
                    }
                    sb.append("\n[output truncated]\n");
                    break;
                }
                sb.append(new String(buffer, 0, read, java.nio.charset.StandardCharsets.UTF_8));
                for (int i = 0; i < read; i++) {
                    if (buffer[i] == '\n') {
                        lines++;
                    }
                    if (lines > MAX_LINES) {
                        sb.append("[output truncated: too many lines]\n");
                        process.destroyForcibly();
                        return sb.toString();
                    }
                }
            }
        } catch (final IOException e) {
            sb.append("\n[read error: ").append(e.getMessage()).append("]\n");
        }
        return sb.toString();
    }
}
