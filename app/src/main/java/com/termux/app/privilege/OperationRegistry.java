package com.termux.app.privilege;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TerminalP-owned registry of approved privileged operations.
 *
 * <p>Only operation identifiers present here may be executed through
 * {@link PrivilegedExecutionService}. The executable, its argument vector, the root
 * requirement, the timeout and the output bound are all pinned at compile time. A caller may
 * not register operations, change arguments, or pass arbitrary command text.</p>
 */
public final class OperationRegistry {

    /** Identifier of the Phase-1 probe operation. */
    public static final String OP_NATIVE_PROBE = "native_probe";

    private static final long DEFAULT_TIMEOUT_MILLIS = 10_000L;
    private static final int DEFAULT_MAX_OUTPUT_BYTES = 64 * 1024;

    private final String prefixDir;
    private final Map<String, OperationSpec> mOperations;

    /**
     * @param prefixDir The PrimeTech prefix directory (e.g. {@code .../files/usr}), without a
     *                  trailing slash. All TerminalP-owned executable paths are derived from it.
     */
    public OperationRegistry(@NonNull final String prefixDir) {
        this.prefixDir = prefixDir;
        final Map<String, OperationSpec> ops = new LinkedHashMap<>();
        ops.put(OP_NATIVE_PROBE, nativeProbeSpec());
        this.mOperations = Collections.unmodifiableMap(ops);
    }

    @NonNull
    private OperationSpec nativeProbeSpec() {
        // Elevation binary bundled in the PrimeTech bootstrap under $PREFIX/bin/sudo.
        final String sudo = prefixDir + "/bin/sudo";
        // TerminalP-owned probe executable, installed at service init into $PREFIX/libexec.
        final String probe = LibexecProbe.PROBE_PATH(prefixDir);
        return new OperationSpec(OP_NATIVE_PROBE, sudo, new String[]{probe},
                true, DEFAULT_TIMEOUT_MILLIS, DEFAULT_MAX_OUTPUT_BYTES);
    }

    /** Returns whether {@code operationId} is registered. */
    public boolean isSupported(@NonNull final String operationId) {
        return mOperations.containsKey(operationId);
    }

    /** Returns the pinned spec for {@code operationId}, or {@code null} if unsupported. */
    @Nullable
    public OperationSpec resolve(@NonNull final String operationId) {
        return mOperations.get(operationId);
    }

    /** The prefix directory this registry was built for. */
    @NonNull
    public String getPrefixDir() {
        return prefixDir;
    }
}
