package kchat.security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

public final class SecurityConfig {
    private SecurityConfig() {}

    public static SSLContext loadServerSslContextIfEnabled() {
        if (!Boolean.getBoolean("kchat.ssl")) {
            return null;
        }
        String ksPath = System.getProperty("kchat.keystore");
        String ksPass = System.getProperty("kchat.keystorePassword");
        if (ksPath == null || ksPass == null) {
            System.err.println("[SecurityConfig] SSL enabled but keystore or password not provided. Continuing without TLS.");
            return null;
        }
        try (FileInputStream fis = new FileInputStream(ksPath)) {
            KeyStore ks = KeyStore.getInstance("JKS");
            char[] password = ksPass.toCharArray();
            ks.load(fis, password);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, password);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
            System.out.println("[SecurityConfig] TLS enabled (server)");
            return ctx;
        } catch (Exception e) {
            System.err.println("[SecurityConfig] Failed to initialize SSLContext: " + e.getMessage());
            return null;
        }
    }

    public static SSLContext loadClientSslContextIfEnabled() {
        if (!Boolean.getBoolean("kchat.ssl")) {
            return null;
        }
        String ksPath = System.getProperty("kchat.keystore");
        String ksPass = System.getProperty("kchat.keystorePassword");
        if (ksPath == null || ksPass == null) {
            System.err.println("[SecurityConfig] Client SSL enabled but keystore or password not provided. Continuing without TLS.");
            return null;
        }
        try (FileInputStream fis = new FileInputStream(ksPath)) {
            KeyStore ks = KeyStore.getInstance("JKS");
            char[] password = ksPass.toCharArray();
            ks.load(fis, password);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tmf.getTrustManagers(), new SecureRandom());
            System.out.println("[SecurityConfig] TLS enabled (client)");
            return ctx;
        } catch (Exception e) {
            System.err.println("[SecurityConfig] Failed to initialize client SSLContext: " + e.getMessage());
            return null;
        }
    }
}

