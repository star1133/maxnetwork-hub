package com.max12311.maxnetworkhub.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthManagerTest {

    @Test
    void verifiesSha256PinAndRejectsWrongPin(@TempDir Path tempDir) throws IOException {
        AuthManager auth = new AuthManager(null, tempDir.toFile());
        UUID uuid = UUID.randomUUID();
        File playerFile = new File(tempDir.resolve("playerdata").toFile(), uuid + ".dat");

        // Write a SHA-256 (v2) PIN file for "1234"
        // SHA-256("maxnetwork:1234") = 728cbf26d6dc22b35208e53681697f68618366c9269e5b85099ac71ee2700c33
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(playerFile))) {
            out.writeByte(2); // VERSION_SHA256
            out.writeUTF("728cbf26d6dc22b35208e53681697f68618366c9269e5b85099ac71ee2700c33");
            out.writeUTF("TestPlayer");
        }

        assertTrue(auth.verifyPin(uuid, "1234"), "Correct PIN should verify");
        assertFalse(auth.verifyPin(uuid, "9999"), "Incorrect PIN should fail");
    }

    @Test
    void upgradesLegacyHashCodePinToSha256OnVerification(@TempDir Path tempDir) throws IOException {
        AuthManager auth = new AuthManager(null, tempDir.toFile());
        UUID uuid = UUID.randomUUID();
        File playerFile = new File(tempDir.resolve("playerdata").toFile(), uuid + ".dat");

        // Write a legacy (v1) PIN file using Integer.toHexString(pin.hashCode())
        String legacyHash = Integer.toHexString("5678".hashCode());
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(playerFile))) {
            out.writeByte(1); // VERSION_LEGACY
            out.writeUTF(legacyHash);
            out.writeUTF("LegacyPlayer");
        }

        assertTrue(auth.verifyPin(uuid, "5678"), "Legacy PIN should verify successfully");

        // Verify that the file was automatically upgraded to VERSION_SHA256
        byte[] bytes = java.nio.file.Files.readAllBytes(playerFile.toPath());
        assertEquals(2, bytes[0], "First byte must be VERSION_SHA256 (2)");

        // Subsequent verification should still pass with upgraded format
        assertTrue(auth.verifyPin(uuid, "5678"), "Upgraded PIN should continue verifying");
    }
}
