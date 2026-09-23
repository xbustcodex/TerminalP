package com.termux.app.privilege;

import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class CallerAuthenticatorTest {

    private static final String PKG = "com.primetechterminal";
    private static final String DIGEST =
            "f4c0c061811d9015e7171ecafec6d0d3963d8332195fb8071cf75d5791d082a1";

    private static final class FakeResolver implements CallerCertificateResolver {
        private final String pkg;
        private final List<String> digests;
        private final int forUid;

        FakeResolver(String pkg, List<String> digests, int forUid) {
            this.pkg = pkg;
            this.digests = digests;
            this.forUid = forUid;
        }

        @Nullable
        @Override
        public String packageNameForUid(int callingUid) {
            return callingUid == forUid ? pkg : null;
        }

        @NonNull
        @Override
        public List<String> signingCertSha256Digests(String packageName) {
            return pkg != null && pkg.equals(packageName) ? digests : Collections.emptyList();
        }
    }

    private CallerAuthenticator authenticator(CallerCertificateResolver resolver) {
        return new CallerAuthenticator(PKG, Collections.singletonList(DIGEST), resolver);
    }

    @Test
    public void authorizedMatchingPackageAndDigest() {
        CallerAuthenticator auth = authenticator(
                new FakeResolver(PKG, Collections.singletonList(DIGEST), 10001));
        assertEquals(PrivilegeStatusCode.STATUS_OK, auth.authenticate(10001));
    }

    @Test
    public void unauthorizedWrongDigest() {
        CallerAuthenticator auth = authenticator(
                new FakeResolver(PKG, Collections.singletonList("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"), 10001));
        assertEquals(PrivilegeStatusCode.STATUS_CALLER_NOT_AUTHORIZED, auth.authenticate(10001));
    }

    @Test
    public void unauthorizedWrongPackage() {
        CallerAuthenticator auth = authenticator(
                new FakeResolver("com.someone.else", Collections.singletonList(DIGEST), 10002));
        assertEquals(PrivilegeStatusCode.STATUS_CALLER_NOT_AUTHORIZED, auth.authenticate(10002));
    }

    @Test
    public void unauthorizedUnresolvedCaller() {
        CallerAuthenticator auth = authenticator(new FakeResolver(null, Collections.emptyList(), 0));
        assertEquals(PrivilegeStatusCode.STATUS_CALLER_UNAVAILABLE, auth.authenticate(9999));
    }

    @Test
    public void unauthorizedMissingCertificates() {
        CallerAuthenticator auth = authenticator(
                new FakeResolver(PKG, Collections.emptyList(), 10001));
        assertEquals(PrivilegeStatusCode.STATUS_CALLER_NOT_AUTHORIZED, auth.authenticate(10001));
    }
}
