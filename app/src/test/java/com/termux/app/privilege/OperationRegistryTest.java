package com.termux.app.privilege;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OperationRegistryTest {

    private static final String PREFIX = "/data/data/com.primetech.terminal/files/usr";

    @Test
    public void nativeProbeIsSupported() {
        OperationRegistry registry = new OperationRegistry(PREFIX);
        assertTrue(registry.isSupported(OperationRegistry.OP_NATIVE_PROBE));
    }

    @Test
    public void unknownOperationIsNotSupported() {
        OperationRegistry registry = new OperationRegistry(PREFIX);
        assertFalse(registry.isSupported("rm_rf"));
        assertFalse(registry.isSupported(""));
        assertNull(registry.resolve("rm_rf"));
    }

    @Test
    public void nativeProbeResolvesToPinnedSudoArgv() {
        OperationRegistry registry = new OperationRegistry(PREFIX);
        OperationSpec spec = registry.resolve(OperationRegistry.OP_NATIVE_PROBE);
        assertNotNull(spec);
        // Elevation goes through the TerminalP-owned bootstrap sudo, never a caller-supplied path.
        assertEquals(PREFIX + "/bin/sudo", spec.execPath);
        // The only argument is the TerminalP-owned probe executable path; no caller-controlled strings.
        assertArrayEquals(new String[]{PREFIX + "/libexec/primetech-native-elf-probe"}, spec.args);
        assertTrue(spec.requiresRoot);
        assertTrue(spec.timeoutMillis > 0);
        assertTrue(spec.maxOutputBytes > 0);
        assertEquals(OperationRegistry.OP_NATIVE_PROBE, spec.operationId);
    }

    @Test
    public void registryUsesGivenPrefix() {
        OperationRegistry registry = new OperationRegistry("/custom/prefix");
        assertEquals("/custom/prefix", registry.getPrefixDir());
        OperationSpec spec = registry.resolve(OperationRegistry.OP_NATIVE_PROBE);
        assertNotNull(spec);
        assertEquals("/custom/prefix/bin/sudo", spec.execPath);
        assertArrayEquals(new String[]{"/custom/prefix/libexec/primetech-native-elf-probe"}, spec.args);
    }
}
