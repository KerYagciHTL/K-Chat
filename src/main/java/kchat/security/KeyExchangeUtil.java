package kchat.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.KeyAgreement;

public final class KeyExchangeUtil {
    private static final String CURVE = "X25519";
    private static final String HMAC_ALG = "HmacSHA256";

    private KeyExchangeUtil() {}

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(CURVE);
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate X25519 key pair", e);
        }
    }

    public static byte[] deriveSharedSecret(PrivateKey privateKey, byte[] peerPublic) {
        try {
            KeyFactory kf = KeyFactory.getInstance(CURVE);
            PublicKey peerPub = kf.generatePublic(new X509EncodedKeySpec(peerPublic));
            KeyAgreement ka = KeyAgreement.getInstance(CURVE);
            ka.init(privateKey);
            ka.doPhase(peerPub, true);
            return ka.generateSecret(); // 32 bytes
        } catch (Exception e) {
            throw new RuntimeException("Failed X25519 agreement", e);
        }
    }

    public static byte[] hkdf(byte[] ikm, byte[] salt, byte[] info, int length) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(salt, HMAC_ALG));
            byte[] prk = mac.doFinal(ikm);
            byte[] okm = new byte[length];
            byte[] prev = new byte[0];
            int pos = 0;
            int counter = 1;
            while (pos < length) {
                mac.init(new SecretKeySpec(prk, HMAC_ALG));
                mac.update(prev);
                mac.update(info);
                mac.update((byte) counter);
                prev = mac.doFinal();
                int toCopy = Math.min(prev.length, length - pos);
                System.arraycopy(prev, 0, okm, pos, toCopy);
                pos += toCopy;
                counter++;
            }
            return okm;
        } catch (Exception e) {
            throw new RuntimeException("HKDF failure", e);
        }
    }

    public static String b64(byte[] data) { return Base64.getEncoder().encodeToString(data); }
    public static byte[] b64d(String s) { return Base64.getDecoder().decode(s); }
}
