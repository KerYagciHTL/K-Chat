package kchat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import kchat.model.Message;
import kchat.security.CryptoUtils;
import kchat.security.SecurityConfig;
import kchat.security.KeyExchangeUtil;

import javax.net.ssl.SSLContext;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class MessengerClient extends WebSocketClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Consumer<Message> messageHandler;
    private Consumer<String> connectionStatusHandler;
    private boolean encryptionEnabled = false;

    // Handshake fields
    private String targetServerId;
    private KeyPair clientKeyPair;
    private boolean handshakeComplete = false;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ScheduledExecutorService HS_EXEC = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "kchat-handshake-timer"); t.setDaemon(true); return t; });
    private ScheduledFuture<?> handshakeTimeoutFuture;
    private static final long HANDSHAKE_TIMEOUT_MS = 5000L;
    private volatile boolean errorSet = false;

    public MessengerClient(URI serverUri) {
        super(serverUri);
        SSLContext ctx = SecurityConfig.loadClientSslContextIfEnabled();
        if (ctx != null) {
            try { this.setSocketFactory(ctx.getSocketFactory()); } catch (Exception e) { System.err.println("Failed to apply client SSL context: " + e.getMessage()); }
        }
    }

    public void setTargetServerId(String serverId) { this.targetServerId = serverId; }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("Socket connected; initiating handshake");
        if (targetServerId == null || targetServerId.isEmpty()) {
            failStatus("No serverId provided");
            close();
            return;
        }
        try {
            clientKeyPair = KeyExchangeUtil.generateKeyPair();
            String pubB64 = Base64.getEncoder().encodeToString(clientKeyPair.getPublic().getEncoded());
            Message hello = new Message("Client", "HELLO:" + targetServerId + ":" + pubB64, System.currentTimeMillis());
            send(objectMapper.writeValueAsString(hello));
            updateStatus("Handshake sent");
            scheduleHandshakeTimeout();
        } catch (Exception e) {
            failStatus("Handshake init failed: " + e.getMessage());
            close();
        }
    }

    @Override
    public void onMessage(String raw) {
        try {
            Message msg = objectMapper.readValue(raw, Message.class);
            String content = msg.getContent();
            if (!handshakeComplete && content != null && content.startsWith("WELCOME:")) {
                handleWelcome(content);
                return;
            }
            if (encryptionEnabled) {
                String decrypted = CryptoUtils.decrypt(msg.getContent());
                msg.setContent(decrypted);
            }
            if (messageHandler != null) {
                messageHandler.accept(msg);
            }
        } catch (Exception e) {
            System.err.println("Error parsing message: " + e.getMessage());
        }
    }

    private void scheduleHandshakeTimeout() {
        if (handshakeTimeoutFuture != null) handshakeTimeoutFuture.cancel(true);
        handshakeTimeoutFuture = HS_EXEC.schedule(() -> {
            if (!handshakeComplete) {
                failStatus("Handshake timeout");
                try { close(); } catch (Exception ignore) {}
            }
        }, HANDSHAKE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    private void handleWelcome(String content) {
        try {
            // Format: WELCOME:serverId:serverPubB64:wrappedGroupKeyB64
            String[] parts = content.split(":", 4);
            if (parts.length < 4) {
                failStatus("Malformed WELCOME");
                close();
                return;
            }
            String srvId = parts[1];
            if (!srvId.equals(targetServerId)) {
                failStatus("ServerId mismatch");
                close();
                return;
            }
            byte[] serverPubEnc = Base64.getDecoder().decode(parts[2]);
            byte[] wrapped = Base64.getDecoder().decode(parts[3]);
            byte[] shared = KeyExchangeUtil.deriveSharedSecret(clientKeyPair.getPrivate(), serverPubEnc);
            byte[] keyWrapKey = KeyExchangeUtil.hkdf(shared, srvId.getBytes(), "kchat-handshake".getBytes(), 32);
            byte[] groupKey = unwrapGroupKey(keyWrapKey, wrapped);
            if (groupKey == null) {
                failStatus("Group key unwrap failed");
                close();
                return;
            }
            CryptoUtils.setRawKey(groupKey);
            handshakeComplete = true;
            if (handshakeTimeoutFuture != null) handshakeTimeoutFuture.cancel(true);
            setEncryptionEnabled(true);
            updateStatus("Connected (Secured)");
        } catch (Exception e) {
            failStatus("WELCOME handling failed: " + e.getMessage());
            close();
        }
    }

    private byte[] unwrapGroupKey(byte[] keyWrapKey, byte[] wrapped) {
        try {
            if (wrapped.length < 13) return null; // iv(12)+tag at least 1
            byte[] iv = new byte[12];
            byte[] ct = new byte[wrapped.length - 12];
            System.arraycopy(wrapped, 0, iv, 0, 12);
            System.arraycopy(wrapped, 12, ct, 0, ct.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyWrapKey, "AES"), new GCMParameterSpec(128, iv));
            return cipher.doFinal(ct);
        } catch (Exception e) {
            System.err.println("Group key unwrap error: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Connection closed: " + reason);
        if (handshakeTimeoutFuture != null) handshakeTimeoutFuture.cancel(true);
        if (!handshakeComplete && !errorSet) {
            String r = reason == null ? "" : reason.toLowerCase();
            if (r.contains("invalid serverid")) {
                failStatus("Invalid server ID");
            } else if (r.contains("handshake")) {
                failStatus("Handshake failed");
            } else if (!r.isEmpty()) {
                failStatus("Closed before handshake: " + reason);
            } else {
                failStatus("Server ID not accepted");
            }
        } else if (!errorSet) {
            if (connectionStatusHandler != null) connectionStatusHandler.accept("Disconnected");
        }
        handshakeComplete = false;
        encryptionEnabled = false;
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("Client error: " + ex.getMessage());
        if (connectionStatusHandler != null) connectionStatusHandler.accept("Error: " + ex.getMessage());
    }

    public void sendMessage(String sender, String content) {
        if (!handshakeComplete) {
            System.err.println("Cannot send before handshake completes");
            return;
        }
        try {
            if (encryptionEnabled) content = CryptoUtils.encrypt(content);
            Message message = new Message(sender, content, System.currentTimeMillis());
            send(objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    public void setMessageHandler(Consumer<Message> handler) { this.messageHandler = handler; }
    public void setConnectionStatusHandler(Consumer<String> handler) { this.connectionStatusHandler = handler; }

    public void setEncryptionEnabled(boolean enabled) { this.encryptionEnabled = enabled && CryptoUtils.isEnabled(); }

    private void updateStatus(String status) { if (connectionStatusHandler != null) connectionStatusHandler.accept(status); }
    private void failStatus(String status) {
        errorSet = true;
        if (connectionStatusHandler != null) connectionStatusHandler.accept("Error: " + status);
    }
}
