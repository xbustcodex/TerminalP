package com.termux.app.privilege;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.logger.Logger;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link CallerCertificateResolver} backed by {@link android.content.pm.PackageManager}.
 * See {@link #signingCertSha256Digests(String)} for the digest format, which matches the
 * value reported by {@code apksigner verify --print-certs} (lowercase hex, no colons).
 */
public final class PackageManagerCertificateResolver implements CallerCertificateResolver {

    private static final String LOG_TAG = "CallerAuth";

    private final Context mContext;

    public PackageManagerCertificateResolver(@NonNull final Context context) {
        this.mContext = context.getApplicationContext();
    }

    @Nullable
    @Override
    public String packageNameForUid(final int callingUid) {
        final String[] packages = mContext.getPackageManager().getPackagesForUid(callingUid);
        if (packages == null || packages.length == 0) {
            return null;
        }
        return packages[0];
    }

    @NonNull
    @Override
    public List<String> signingCertSha256Digests(@NonNull final String packageName) {
        final List<String> digests = new ArrayList<>();
        try {
            final PackageInfo packageInfo;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo = mContext.getPackageManager().getPackageInfo(packageName,
                        PackageManager.GET_SIGNING_CERTIFICATES);
            } else {
                packageInfo = mContext.getPackageManager().getPackageInfo(packageName,
                        PackageManager.GET_SIGNATURES);
            }

            final Signature[] certs;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                final SigningInfo signingInfo = packageInfo.signingInfo;
                if (signingInfo == null) {
                    return digests;
                }
                certs = signingInfo.getApkContentsSigners();
            } else {
                certs = packageInfo.signatures;
            }

            if (certs == null) {
                return digests;
            }
            for (final Signature cert : certs) {
                digests.add(sha256Hex(cert.toByteArray()));
            }
        } catch (final PackageManager.NameNotFoundException e) {
            Logger.logError(LOG_TAG, "Package not found for cert check: " + packageName);
        }
        return digests;
    }

    /** Lowercase hex SHA-256, no colons, matching {@code apksigner} output minus formatting. */
    private static String sha256Hex(final byte[] data) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            final byte[] digest = md.digest(data);
            final StringBuilder sb = new StringBuilder(digest.length * 2);
            for (final byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (final java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
