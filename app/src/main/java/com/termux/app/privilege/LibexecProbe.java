package com.termux.app.privilege;

import android.content.Context;
import android.system.Os;

import androidx.annotation.NonNull;

import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Manages the TerminalP-owned probe executable: its location under the PrimeTech prefix and
 * its installation into that location from the TerminalP APK itself.
 *
 * <p>The probe is built by the app's NDK build as a standalone arm64 ELF and packaged into the
 * APK as an asset ({@code assets/primetech/probe/primetech-native-elf-probe}). On service init
 * we write it to a TerminalP-private libexec location under our own bootstrap prefix so that it
 * can be executed through {@code sudo} as root. This keeps every executable the bridge runs
 * TerminalP-owned.</p>
 */
public final class LibexecProbe {

    private static final String LOG_TAG = "LibexecProbe";

    /** Destination basename under {@code $PREFIX/libexec}. */
    public static final String LIBEXEC_BASENAME = "primetech-native-elf-probe";

    private static final String ASSET_PATH = "primetech/probe/" + LIBEXEC_BASENAME;

    private LibexecProbe() {
    }

    /** Absolute path of the TerminalP-owned probe under the given prefix directory. */
    @NonNull
    public static String PROBE_PATH(@NonNull final String prefixDir) {
        return prefixDir + "/libexec/" + LIBEXEC_BASENAME;
    }

    /**
     * Installs the APK asset probe into {@code $PREFIX/libexec} with execute permissions.
     *
     * @return {@code true} if the probe is present and executable afterwards.
     */
    public static boolean installIfNeeded(@NonNull final Context context, @NonNull final String prefixDir) {
        final File target = new File(PROBE_PATH(prefixDir));
        if (target.isFile() && target.canExecute()) {
            return true;
        }

        final File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            Logger.logError(LOG_TAG, "Failed to create libexec dir " + parent);
            return false;
        }

        try (final InputStream in = context.getAssets().open(ASSET_PATH)) {
            try (final FileOutputStream out = new FileOutputStream(target)) {
                final byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
            setExecutable(target);
            Logger.logVerbose(LOG_TAG, "Installed probe to " + target);
            return target.canExecute();
        } catch (final IOException e) {
            Logger.logError(LOG_TAG, "Failed to install probe to " + target + ": " + e);
            return false;
        }
    }

    private static void setExecutable(@NonNull final File file) throws IOException {
        try {
            Os.chmod(file.getAbsolutePath(), 0700);
        } catch (final Exception e) {
            if (!file.setExecutable(true, true)) {
                throw new IOException("Failed to make probe executable: " + file, e);
            }
        }
    }
}
