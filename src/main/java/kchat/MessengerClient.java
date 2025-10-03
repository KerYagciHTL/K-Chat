package kchat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import kchat.model.Message;
import kchat.security.CryptoUtils;
import kchat.security.SecurityConfig;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.util.function.Consumer;

public class MessengerClient extends WebSocketClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Consumer<Message> messageHandler;
    private Consumer<String> connectionStatusHandler;
    private boolean encryptionEnabled = false;

    public MessengerClient(URI serverUri) {
        super(serverUri);
        SSLContext ctx = SecurityConfig.loadClientSslContextIfEnabled();
        if (ctx != null) {
            try {
                this.setSocketFactory(ctx.getSocketFactory());
            } catch (Exception e) {
                System.err.println("Failed to apply client SSL context: " + e.getMessage());
            }
        }
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("Connected to server");
        if (connectionStatusHandler != null) {
            connectionStatusHandler.accept("Connected" + (encryptionEnabled ? " (Encrypted)" : ""));
        }
    }

    @Override
    public void onMessage(String message) {
        try {
            Message msg = objectMapper.readValue(message, Message.class);
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

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Connection closed: " + reason);
        if (connectionStatusHandler != null) {
            connectionStatusHandler.accept("Disconnected");
        }
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("Client error: " + ex.getMessage());
        if (connectionStatusHandler != null) {
            connectionStatusHandler.accept("Error: " + ex.getMessage());
        }
    }

    public void sendMessage(String sender, String content) {
        try {
            if (encryptionEnabled) {
                content = CryptoUtils.encrypt(content);
            }
            Message message = new Message(sender, content, System.currentTimeMillis());
            String jsonMessage = objectMapper.writeValueAsString(message);
            send(jsonMessage);
        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    public void setMessageHandler(Consumer<Message> handler) {
        this.messageHandler = handler;
    }

    public void setConnectionStatusHandler(Consumer<String> handler) {
        this.connectionStatusHandler = handler;
    }

    public void setEncryptionEnabled(boolean enabled) {
        this.encryptionEnabled = enabled && CryptoUtils.isEnabled();
    }
}
