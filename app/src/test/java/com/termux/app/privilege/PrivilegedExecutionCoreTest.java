package com.termux.app.privilege;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
public class PrivilegedExecutionCoreTest {

    private static final String PKG = PrivilegePolicy.AUTHORIZED_PACKAGE;
    private static final String DIGEST = PrivilegePolicy.AUTHORIZED_CERT_SHA256;

    /** Fake resolver that authorizes a single uid. */
    private static final class FakeResolver implements CallerCertificateResolver {
        private final int uid;

        FakeResolver(int uid) {
            this.uid = uid;
        }

        @Nullable
        @Override
        public String packageNameForUid(int callingUid) {
            return callingUid == uid ? PKG : null;
        }

        @NonNull
        @Override
        public List<String> signingCertSha256Digests(String packageName) {
            return PKG.equals(packageName) ? Collections.singletonList(DIGEST) : Collections.emptyList();
        }
    }

    /** Fake runner that records the exact argv/env it received and returns a fixed result. */
    private static final class RecordingRunner implements PrivilegedProcessRunner {
        final AtomicReference<String> execRef = new AtomicReference<>();
        final AtomicReference<String[]> argsRef = new AtomicReference<>();
        final AtomicReference<Map<String, String>> envRef = new AtomicReference<>();
        ProcessRunResult result;

        @NonNull
        @Override
        public ProcessRunResult run(@NonNull String executable, @NonNull String[] args,
                                    @NonNull Map<String, String> environment,
                                    long timeoutMillis, int maxOutputBytes) {
            execRef.set(executable);
            argsRef.set(args);
            envRef.set(environment);
            return result != null ? result : ProcessRunResult.success(0, "");
        }
    }

    @Test
    public void processNativeProbeRoutesPinnedSudoArgv() {
        Context context = RuntimeEnvironment.getApplication().getApplicationContext();
        String prefix = "/data/data/com.primetech.terminal/files/usr";
        RecordingRunner runner = new RecordingRunner();
        runner.result = ProcessRunResult.success(0, "PRIMETECH_ELF_OK\n");
        PrivilegedExecutionService.PrivilegedExecutionCore core =
                new PrivilegedExecutionService.PrivilegedExecutionCore(context, prefix, runner,
                        new FakeResolver(10001));

        OperationSpec spec = new OperationRegistry(prefix).resolve(OperationRegistry.OP_NATIVE_PROBE);
        assertNotNull(spec);

        ProcessRunResult run = core.runner().run(spec.execPath, spec.args,
                core.environmentFor(spec), spec.timeoutMillis, spec.maxOutputBytes);

        assertEquals(prefix + "/bin/sudo", runner.execRef.get());
        assertArrayEquals(new String[]{prefix + "/libexec/primetech-native-elf-probe"}, runner.argsRef.get());
        assertTrue(run.started);
        assertFalse(run.timedOut);
        assertEquals(0, run.exitCode);
        assertEquals("PRIMETECH_ELF_OK\n", run.stdout);
    }

    @Test
    public void environmentContainsBootstrapVars() {
        Context context = RuntimeEnvironment.getApplication().getApplicationContext();
        String prefix = "/data/data/com.primetech.terminal/files/usr";
        RecordingRunner runner = new RecordingRunner();
        runner.result = ProcessRunResult.success(0, "");
        PrivilegedExecutionService.PrivilegedExecutionCore core =
                new PrivilegedExecutionService.PrivilegedExecutionCore(context, prefix, runner,
                        new FakeResolver(10001));
        OperationSpec spec = new OperationRegistry(prefix).resolve(OperationRegistry.OP_NATIVE_PROBE);

        Map<String, String> env = core.environmentFor(spec);
        assertEquals(prefix, env.get("PREFIX"));
        assertEquals(prefix, env.get("TERMUX_PREFIX"));
        assertEquals(prefix + "/../home", env.get("HOME"));
        assertTrue(env.get("PATH").startsWith(prefix + "/bin:"));
        assertEquals(prefix + "/lib", env.get("LD_LIBRARY_PATH"));
    }

    @Test
    public void unauthorizedCallerRejectedBeforeRun() {
        Context context = RuntimeEnvironment.getApplication().getApplicationContext();
        RecordingRunner runner = new RecordingRunner();
        PrivilegedExecutionService.PrivilegedExecutionCore core =
                new PrivilegedExecutionService.PrivilegedExecutionCore(context, "/prefix", runner,
                        new FakeResolver(10001));
        // A caller that is not the authorized uid 10001 must be rejected.
        assertEquals(PrivilegeStatusCode.STATUS_CALLER_UNAVAILABLE, core.authenticate(9999));
        assertEquals(PrivilegeStatusCode.STATUS_OK, core.authenticate(10001));
    }
}
