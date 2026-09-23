package com.termux.app.privilege;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.primetech.terminal.privilege.IPrivilegedExecution;
import com.primetech.terminal.privilege.IPrivilegedExecutionCallback;

/**
 * Bound service that runs TerminalP-owned privileged (root) operations on behalf of the
 * signed PrimeTech Terminal application.
 *
 * <p>It performs <b>server-side certified-authentication</b> of the binder caller (package +
 * signing certificate SHA-256 digest), resolves the request against a TerminalP-owned
 * operation registry, and executes the allowlisted argv via {@code sudo}. The caller may only
 * request a registered {@code operationId}; it can never supply an executable path, arguments,
 * or an arbitrary shell string, so this service is not a generic root shell.</p>
 *
 * <p>This is deliberately distinct from {@link com.termux.app.RunCommandService}, which stays
 * untouched.</p>
 */
public final class PrivilegedExecutionService extends Service {

    private static final String LOG_TAG = "PrivilegedExec";

    private ExecutorService mExecutor;
    private PrivilegedExecutionCore mCore;

    @Override
    public void onCreate() {
        super.onCreate();
        mExecutor = Executors.newSingleThreadExecutor();
        mCore = new PrivilegedExecutionCore(this, TermuxConstants.TERMUX_PREFIX_DIR_PATH);
    }

    @Override
    public void onDestroy() {
        mExecutor.shutdownNow();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(final Intent intent) {
        return mBinder;
    }

    private final IPrivilegedExecution.Stub mBinder = new IPrivilegedExecution.Stub() {

        @Override
        public boolean isOperationSupported(final String operationId) {
            final int callingUid = Binder.getCallingUid();
            if (mCore.authenticate(callingUid) != PrivilegeStatusCode.STATUS_OK) {
                return false;
            }
            return mCore.isSupported(operationId);
        }

        @Override
        public void execute(final String operationId, final String correlationId,
                            final IPrivilegedExecutionCallback callback) {
            final int callingUid = Binder.getCallingUid();
            final int auth = mCore.authenticate(callingUid);
            if (auth != PrivilegeStatusCode.STATUS_OK) {
                deliver(callback, operationId, correlationId, auth,
                        statusMessage(auth), "", -1);
                return;
            }

            // Resolve + filesystem-validate on the binder thread so the decision is made
            // before any background work touches the allowlisted executable.
            final OperationSpec spec = mCore.resolveValidated(operationId);
            if (spec == null) {
                final int code = mCore.isSupported(operationId)
                        ? PrivilegeStatusCode.STATUS_EXECUTION_INVALID
                        : PrivilegeStatusCode.STATUS_OPERATION_NOT_SUPPORTED;
                deliver(callback, operationId, correlationId, code,
                        statusMessage(code), "", -1);
                return;
            }

            final String resolvedExec = spec.execPath;
            final String[] resolvedArgs = spec.args;
            final Map<String, String> env = mCore.environmentFor(spec);
            final long timeout = spec.timeoutMillis;
            final int maxOutput = spec.maxOutputBytes;

            mExecutor.execute(() -> {
                final ProcessRunResult run = mCore.runner().run(resolvedExec, resolvedArgs, env,
                        timeout, maxOutput);
                int code = PrivilegeStatusCode.STATUS_OK;
                String detail = "";
                if (!run.started) {
                    code = PrivilegeStatusCode.STATUS_EXECUTION_FAILED;
                    detail = run.error;
                } else if (run.timedOut) {
                    code = PrivilegeStatusCode.STATUS_TIMEOUT;
                }
                mCore.audit(callingUid, operationId, correlationId, code);
                deliver(callback, operationId, correlationId, code, detail, run.stdout, run.exitCode);
            });
        }
    };

    private static void deliver(@Nullable final IPrivilegedExecutionCallback callback,
                                final String operationId, final String correlationId,
                                final int statusCode, final String statusMessage,
                                final String stdout, final int exitCode) {
        if (callback == null) {
            return;
        }
        try {
            callback.onResult(operationId, correlationId, statusCode, statusMessage, stdout, exitCode);
        } catch (final RemoteException e) {
            Logger.logError(LOG_TAG, "Failed to deliver result: " + e);
        }
    }

    private static String statusMessage(final int code) {
        switch (code) {
            case PrivilegeStatusCode.STATUS_CALLER_UNAVAILABLE:
                return "CALLER_UNAVAILABLE";
            case PrivilegeStatusCode.STATUS_CALLER_NOT_AUTHORIZED:
                return "CALLER_NOT_AUTHORIZED";
            case PrivilegeStatusCode.STATUS_OPERATION_NOT_SUPPORTED:
                return "OPERATION_NOT_SUPPORTED";
            case PrivilegeStatusCode.STATUS_OPERATION_UNAVAILABLE:
                return "OPERATION_UNAVAILABLE";
            case PrivilegeStatusCode.STATUS_EXECUTION_INVALID:
                return "EXECUTION_INVALID";
            case PrivilegeStatusCode.STATUS_EXECUTION_FAILED:
                return "EXECUTION_FAILED";
            case PrivilegeStatusCode.STATUS_TIMEOUT:
                return "TIMEOUT";
            default:
                return "OK";
        }
    }

    /** Foundation used by the service; separated for unit testing without Android IPC. */
    static final class PrivilegedExecutionCore {

        private final Context mContext;
        private final String mPrefixDir;
        private final CallerAuthenticator mAuthenticator;
        private final OperationRegistry mRegistry;
        private final PrivilegedProcessRunner mRunner;

        PrivilegedExecutionCore(@NonNull final Context context, @NonNull final String prefixDir) {
            this.mContext = context.getApplicationContext();
            this.mPrefixDir = prefixDir;
            this.mAuthenticator = new CallerAuthenticator(PrivilegePolicy.AUTHORIZED_PACKAGE,
                    PrivilegePolicy.authorizedCertSha256Digests(),
                    new PackageManagerCertificateResolver(context));
            this.mRegistry = new OperationRegistry(prefixDir);
            this.mRunner = new OsProcessRunner();
            LibexecProbe.installIfNeeded(context, prefixDir);
        }

        /** Constructor for tests with a fake runner and resolver. */
        PrivilegedExecutionCore(@NonNull final Context context, @NonNull final String prefixDir,
                                @NonNull final PrivilegedProcessRunner runner,
                                @NonNull final CallerCertificateResolver resolver) {
            this.mContext = context.getApplicationContext();
            this.mPrefixDir = prefixDir;
            this.mAuthenticator = new CallerAuthenticator(PrivilegePolicy.AUTHORIZED_PACKAGE,
                    PrivilegePolicy.authorizedCertSha256Digests(), resolver);
            this.mRegistry = new OperationRegistry(prefixDir);
            this.mRunner = runner;
        }

        int authenticate(final int callingUid) {
            return mAuthenticator.authenticate(callingUid);
        }

        boolean isSupported(final String operationId) {
            return mRegistry.isSupported(operationId);
        }

        /** Returns the validated spec for {@code operationId} or {@code null} if unusable. */
        @Nullable
        OperationSpec resolveValidated(final String operationId) {
            final OperationSpec spec = mRegistry.resolve(operationId);
            if (spec == null) {
                return null;
            }
            return validateOnDisk(mPrefixDir, spec) ? spec : null;
        }

        @NonNull
        PrivilegedProcessRunner runner() {
            return mRunner;
        }

        @NonNull
        Map<String, String> environmentFor(@NonNull final OperationSpec spec) {
            final Map<String, String> env = new HashMap<>();
            env.put("PREFIX", mPrefixDir);
            env.put("TERMUX_PREFIX", mPrefixDir);
            env.put("HOME", mPrefixDir + "/../home");
            env.put("TERMUX_APP_PACKAGE_NAME", TermuxConstants.TERMUX_PACKAGE_NAME);
            env.put("PATH", mPrefixDir + "/bin:" + mPrefixDir + "/bin/applets:/system/bin");
            env.put("LD_LIBRARY_PATH", mPrefixDir + "/lib");
            return env;
        }

        void audit(final int callingUid, final String operationId, final String correlationId,
                   final int statusCode) {
            Logger.logVerbose(LOG_TAG, "audit uid=" + callingUid + " op=" + operationId +
                    " corr=" + correlationId + " status=" + statusCode);
        }

        @NonNull
        String prefixDir() {
            return mPrefixDir;
        }

        @Nullable
        Context context() {
            return mContext;
        }
    }

    /**
     * Validates that an allowlisted executable path is safe to execute: it must resolve to a
     * regular, executable file under the TerminalP-owned prefix/libexec space with no symlink
     * escape outside the prefix.
     */
    static boolean validateOnDisk(@NonNull final String prefixDir, @NonNull final OperationSpec spec) {
        try {
            final File execFile = new File(spec.execPath).getCanonicalFile();
            if (!isUnderPrefix(prefixDir, execFile)) {
                return false;
            }
            for (final String arg : spec.args) {
                final File argFile = new File(arg).getCanonicalFile();
                if (!isUnderPrefix(prefixDir, argFile)) {
                    return false;
                }
            }
            final File exec = new File(spec.execPath);
            if (!exec.isFile() || !exec.canExecute()) {
                return false;
            }
        } catch (final IOException e) {
            return false;
        }
        return true;
    }

    private static boolean isUnderPrefix(@NonNull final String prefixDir, @NonNull final File canonical) {
        final String canonicalPrefix;
        try {
            canonicalPrefix = new File(prefixDir).getCanonicalPath();
        } catch (final IOException e) {
            return false;
        }
        final String path = canonical.getPath();
        return path.equals(canonicalPrefix) || path.startsWith(canonicalPrefix + File.separator);
    }
}
