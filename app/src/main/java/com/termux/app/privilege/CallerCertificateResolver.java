package com.termux.app.privilege;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Resolves a binder calling UID to the SHA-256 signing certificate digest of the calling
 * app. TerminalP uses this at request time to attest that a caller is the signed PrimeTech
 * Terminal application. The system does not grant an OS-level signature permission between
 * the two differently-signed apps, so TerminalP verifies the certificate itself.
 */
public interface CallerCertificateResolver {

    /**
     * @param callingUid The binder caller UID (e.g. {@code Binder.getCallingUid()}).
     * @return The package name of the app owning {@code callingUid}, or {@code null} if the
     *         UID maps to no installable package (e.g. the kernel/root or a non-app UID).
     */
    @Nullable
    String packageNameForUid(int callingUid);

    /**
     * @param packageName A package installed on the device.
     * @return All signing certificate SHA-256 digests (lowercase hex, no colons) of the
     *         package, or an empty list if unavailable.
     */
    @NonNull
    List<String> signingCertSha256Digests(String packageName);
}
