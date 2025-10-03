package kchat.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

public final class CryptoUtils {
    private static final String ENC_PREFIX = "ENC:";
    private static final byte[] SALT = "KchatFixedSaltV1".getBytes(StandardCharsets.UTF_8);
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH = 256; // bits
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12; // GCM 96-bit nonce

    private static volatile SecretKeySpec cachedKey;
    private static volatile String currentPassphrase;
    private static final SecureRandom RANDOM = new SecureRandom();

    private CryptoUtils() {}

    public static void setPassphrase(String passphrase) {
        if (passphrase == null || passphrase.isEmpty()) {
            clearPassphrase();
            return;
        }
        currentPassphrase = passphrase;
        cachedKey = null;
    }

    public static void clearPassphrase() {
        currentPassphrase = null;
        cachedKey = null;
    }

    public static boolean isEnabled() {
        return currentPassphrase != null && !currentPassphrase.isEmpty();
    }

    private static SecretKeySpec deriveKey() throws Exception {
        SecretKeySpec key = cachedKey;
        if (key != null) return key;
        if (currentPassphrase == null) throw new IllegalStateException("Passphrase not set");
        synchronized (CryptoUtils.class) {
            if (cachedKey == null) {
                KeySpec spec = new PBEKeySpec(currentPassphrase.toCharArray(), SALT, ITERATIONS, KEY_LENGTH);
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                byte[] keyBytes = factory.generateSecret(spec).getEncoded();
                cachedKey = new SecretKeySpec(keyBytes, "AES");
            }
            return cachedKey;
        }
    }

    public static String encrypt(String plaintext) {
        if (!isEnabled() || plaintext == null || plaintext.isEmpty()) return plaintext;
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer bb = ByteBuffer.allocate(iv.length + ciphertext.length);
            bb.put(iv).put(ciphertext);
            return ENC_PREFIX + Base64.getEncoder().encodeToString(bb.array());
        } catch (Exception e) {
            System.err.println("[CryptoUtils] Encryption failed: " + e.getMessage());
            return plaintext; // graceful fallback
        }
    }

    public static String decrypt(String maybeEncrypted) {
        if (!isEnabled() || maybeEncrypted == null || !maybeEncrypted.startsWith(ENC_PREFIX)) {
            return maybeEncrypted;
        }
        try {
            byte[] all = Base64.getDecoder().decode(maybeEncrypted.substring(ENC_PREFIX.length()));
            if (all.length <= IV_LENGTH) return maybeEncrypted; // invalid length
            byte[] iv = new byte[IV_LENGTH];
            byte[] ciphertext = new byte[all.length - IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            System.arraycopy(all, IV_LENGTH, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(ciphertext);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[CryptoUtils] Decryption failed: " + e.getMessage());
            return maybeEncrypted;
        }
    }
}

