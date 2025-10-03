package kchat.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CryptoUtilsTest {

    @AfterEach
    void cleanup() {
        CryptoUtils.clearPassphrase();
    }

    @Test
    void testEncryptionRoundTrip() {
        CryptoUtils.setPassphrase("secret123");
        String original = "Hello Secret World";
        String encrypted = CryptoUtils.encrypt(original);
        assertNotEquals(original, encrypted, "Encrypted text should differ from original");
        assertTrue(encrypted.startsWith("ENC:"));
        String decrypted = CryptoUtils.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void testDecryptWithoutSecretReturnsOriginal() {
        String raw = "ENC:SGVsbG8=";
        String result = CryptoUtils.decrypt(raw); // no secret set
        assertEquals(raw, result);
    }

    @Test
    void testEncryptWithoutSecretReturnsPlain() {
        String plain = "NoSecret";
        String out = CryptoUtils.encrypt(plain); // no secret set
        assertEquals(plain, out);
    }
}

