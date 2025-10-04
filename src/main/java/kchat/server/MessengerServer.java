package kchat.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import kchat.model.Message;
import kchat.security.SecurityConfig;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import kchat.security.KeyExchangeUtil;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.SSLContext;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MessengerServer extends WebSocketServer {

    private final Set<WebSocket> connections = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<WebSocket> authenticatedConnections = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Security / handshake fields
    private final String serverId = UUID.randomUUID().toString();
    private final KeyPair serverKeyPair = KeyExchangeUtil.generateKeyPair(); // X25519
    private final byte[] groupKey = new byte[32]; // shared symmetric key for all clients
    private static final SecureRandom RANDOM = new SecureRandom();

    public MessengerServer(int port) {
        super(new InetSocketAddress(port));
        RANDOM.nextBytes(groupKey);
        System.out.println("Messenger Server initialized on port " + port + " (serverId=" + serverId + ")");
        SSLContext sslContext = SecurityConfig.loadServerSslContextIfEnabled();
        if (sslContext != null) {
            setWebSocketFactory(new DefaultSSLWebSocketServerFactory(sslContext));
            System.out.println("Messenger Server running with TLS (wss)");
        }
    }

    public String getServerId() { return serverId; }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        // Removed logging - server should be silent about connections
        // Don't broadcast anything until authentication is complete
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        boolean wasAuthenticated = authenticatedConnections.remove(conn);
        // Removed logging - server should be silent about disconnections

        // Only broadcast leave message and update user count if the user was properly authenticated
        if (wasAuthenticated) {
            broadcastToAuthenticated(createLeaveMessage());
            broadcastUserCountToAuthenticated();
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            Message msg = objectMapper.readValue(message, Message.class);
            String content = msg.getContent();
            if (content != null && content.startsWith("HELLO:")) {
                handleHello(conn, msg);
                return; // do not broadcast handshake messages
            }

            // Only process regular messages from authenticated clients
            if (!authenticatedConnections.contains(conn)) {
                // Silent ignore - no logging about unauthenticated clients
                return;
            }

            msg.setTimestamp(System.currentTimeMillis());
            System.out.println("Received message: " + msg.getContent() + " from " + msg.getSender());
            broadcastToAuthenticated(msg);
        } catch (Exception e) {
            // Only log actual processing errors, not authentication issues
            if (authenticatedConnections.contains(conn)) {
                System.err.println("Error processing message: " + e.getMessage());
            }
        }
    }

    private void handleHello(WebSocket conn, Message msg) {
        try {
            // Format: HELLO:serverId:clientPubB64
            String[] parts = msg.getContent().split(":", 3);
            if (parts.length < 3) {
                // Silent rejection - no logging
                conn.close(1002, "Malformed handshake");
                return;
            }
            String claimedServerId = parts[1];
            if (!serverId.equals(claimedServerId)) {
                // Silent rejection - no logging about wrong server ID
                conn.close(1002, "Invalid serverId");
                return;
            }

            // Authentication successful - add to authenticated connections
            authenticatedConnections.add(conn);

            byte[] clientPubRaw = Base64.getDecoder().decode(parts[2]);
            byte[] sharedSecret = KeyExchangeUtil.deriveSharedSecret(serverKeyPair.getPrivate(), clientPubRaw);
            byte[] keyWrapKey = KeyExchangeUtil.hkdf(sharedSecret, serverId.getBytes(StandardCharsets.UTF_8), "kchat-handshake".getBytes(StandardCharsets.UTF_8), 32);
            String wrapped = wrapGroupKey(keyWrapKey);
            String serverPubB64 = Base64.getEncoder().encodeToString(serverKeyPair.getPublic().getEncoded());
            Message resp = new Message("System", "WELCOME:" + serverId + ":" + serverPubB64 + ":" + wrapped, System.currentTimeMillis());
            conn.send(objectMapper.writeValueAsString(resp));

            // Now broadcast welcome and user count to all authenticated users
            broadcastToAuthenticated(createWelcomeMessage());
            broadcastUserCountToAuthenticated();

        } catch (Exception e) {
            // Silent rejection - no logging about handshake errors
            try { conn.close(1011, "Handshake failure"); } catch (Exception ignore) {}
        }
    }

    private String wrapGroupKey(byte[] keyWrapKey) throws Exception {
        byte[] iv = new byte[12];
        RANDOM.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyWrapKey, "AES"), new GCMParameterSpec(128, iv));
        byte[] ct = cipher.doFinal(groupKey);
        ByteBuffer bb = ByteBuffer.allocate(iv.length + ct.length);
        bb.put(iv).put(ct);
        return Base64.getEncoder().encodeToString(bb.array());
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("WebSocket error: " + ex.getMessage());
        if (conn != null) {
            connections.remove(conn);
            authenticatedConnections.remove(conn);
            broadcastUserCountToAuthenticated();
        }
    }

    @Override
    public void onStart() {
        System.out.println("Messenger Server started successfully!");
        setConnectionLostTimeout(10);
    }

    protected Message createWelcomeMessage() {
        return new Message("Server", "User joined the chat", System.currentTimeMillis());
    }

    protected Message createLeaveMessage() {
        return new Message("Server", "User left the chat", System.currentTimeMillis());
    }

    private void broadcastUserCountToAuthenticated() {
        Message userCountMessage = new Message("System", "USER_COUNT:" + getAuthenticatedConnectionCount(), System.currentTimeMillis());
        broadcastToAuthenticated(userCountMessage);
    }

    protected void broadcastToAuthenticated(Message message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            Set<WebSocket> connectionsCopy = new HashSet<>(authenticatedConnections);
            for (WebSocket conn : connectionsCopy) {
                if (conn.isOpen()) {
                    try {
                        conn.send(jsonMessage);
                    } catch (Exception e) {
                        System.err.println("Error sending to client, removing connection: " + e.getMessage());
                        authenticatedConnections.remove(conn);
                        connections.remove(conn);
                    }
                } else {
                    authenticatedConnections.remove(conn);
                    connections.remove(conn);
                }
            }
        } catch (Exception e) {
            System.err.println("Error broadcasting message: " + e.getMessage());
        }
    }

    // Keep the old broadcast method for backwards compatibility if needed
    protected void broadcast(Message message) {
        broadcastToAuthenticated(message);
    }

    public int getConnectionCount() {
        return connections.size();
    }

    public int getAuthenticatedConnectionCount() {
        return authenticatedConnections.size();
    }

    Message processIncomingRawJson(String rawJson) throws java.io.IOException {
        Message msg = objectMapper.readValue(rawJson, Message.class);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }
}
