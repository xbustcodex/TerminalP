package com.termux.app.privilege;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Server-side caller authentication for the privileged execution bridge.
 *
 * <p>Because TerminalP and PrimeTech Terminal are signed with different keys, Android cannot
 * grant an OS-level {@code protectionLevel="signature"} permission between them. Instead
 * TerminalP resolves each binder caller to its package name and signing certificate SHA-256
 * digest and requires a match against a TerminalP-owned allowlist. This attests the caller is
 * the exact signed PrimeTech Terminal APK.</p>
 */
public final class CallerAuthenticator {

    /** Package that is allowed to invoke privileged operations. */
    @NonNull
    private final String mAuthorizedPackage;

    /** Signing cert SHA-256 digest(s) (lowercase hex, no colons) allowed for that package. */
    @NonNull
    private final List<String> mAuthorizedDigests;

    @NonNull
    private final CallerCertificateResolver mResolver;

    public CallerAuthenticator(@NonNull final String authorizedPackage,
                               @NonNull final List<String> authorizedDigests,
                               @NonNull final CallerCertificateResolver resolver) {
        this.mAuthorizedPackage = authorizedPackage;
        this.mAuthorizedDigests = authorizedDigests;
        this.mResolver = resolver;
    }

    /** Returns an {@link PrivilegeStatusCode} describing whether {@code callingUid} may invoke. */
    public int authenticate(final int callingUid) {
        final String callerPackage = mResolver.packageNameForUid(callingUid);
        if (callerPackage == null) {
            return PrivilegeStatusCode.STATUS_CALLER_UNAVAILABLE;
        }
        if (!mAuthorizedPackage.equals(callerPackage)) {
            return PrivilegeStatusCode.STATUS_CALLER_NOT_AUTHORIZED;
        }
        final List<String> digests = mResolver.signingCertSha256Digests(callerPackage);
        if (digests.isEmpty()) {
            return PrivilegeStatusCode.STATUS_CALLER_NOT_AUTHORIZED;
        }
        for (final String digest : digests) {
            if (mAuthorizedDigests.contains(digest)) {
                return PrivilegeStatusCode.STATUS_OK;
            }
        }
        return PrivilegeStatusCode.STATUS_CALLER_NOT_AUTHORIZED;
    }

    /**
     * Optional caller-provided identity hint for audit logging. Never used for the decision;
     * the authoritative identity always comes from the binder UID.
     */
    @Nullable
    public String resolvePackageName(final int callingUid) {
        return mResolver.packageNameForUid(callingUid);
    }
}
