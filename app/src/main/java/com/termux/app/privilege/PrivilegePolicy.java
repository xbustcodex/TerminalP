package com.termux.app.privilege;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * TerminalP-side policy constants for the privileged execution bridge.
 *
 * <p>The allowlist package and signing certificate digest below identify the exact signed
 * PrimeTech Terminal APK permitted to invoke privileged operations. The digest is the
 * authoritative value reported by {@code apksigner verify --print-certs} for the current
 * PrimeTech Terminal debug build.</p>
 *
 * <p><b>Production note:</b> this SHA-256 is the PrimeTech Terminal <em>debug</em> cert only.
 * Before a PrimeTech Terminal release build (which will use a permanent release keystore and a
 * different certificate) is allowed to invoke the bridge, this allowlist must be updated to the
 * release certificate digest. Release builds should accept only the production identity.</p>
 */
public final class PrivilegePolicy {

    /** PrimeTech Terminal application package. */
    public static final String AUTHORIZED_PACKAGE = "com.primetechterminal";

    /**
     * Authoritative signing certificate SHA-256 digest (lowercase hex, no colons) of the
     * PrimeTech Terminal APK, captured from the currently-built debug APK.
     */
    public static final String AUTHORIZED_CERT_SHA256 =
            "f4c0c061811d9015e7171ecafec6d0d3963d8332195fb8071cf75d5791d082a1";

    @NonNull
    public static List<String> authorizedCertSha256Digests() {
        return Collections.singletonList(AUTHORIZED_CERT_SHA256);
    }

    private PrivilegePolicy() {
    }
}
