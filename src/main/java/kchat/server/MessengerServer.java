package kchat.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import kchat.model.Message;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MessengerServer extends WebSocketServer {

    private final Set<WebSocket> connections = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MessengerServer(int port) {
        super(new InetSocketAddress(port));
        System.out.println("Messenger Server initialized on port " + port);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        System.out.println("New connection: " + conn.getRemoteSocketAddress());
        broadcast(createWelcomeMessage());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        System.out.println("Connection closed: " + conn.getRemoteSocketAddress());
        broadcast(createLeaveMessage());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            Message msg = objectMapper.readValue(message, Message.class);
            msg.setTimestamp(System.currentTimeMillis()); // Server sets timestamp
            System.out.println("Received message: " + msg.getContent() + " from " + msg.getSender());
            broadcast(msg);
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("WebSocket error: " + ex.getMessage());
        if (conn != null) {
            connections.remove(conn);
        }
    }

    @Override
    public void onStart() {
        System.out.println("Messenger Server started successfully!");
    }

    // Visible for subclasses/tests
    protected Message createWelcomeMessage() {
        return new Message("Server", "User joined the chat", System.currentTimeMillis());
    }

    protected Message createLeaveMessage() {
        return new Message("Server", "User left the chat", System.currentTimeMillis());
    }

    protected void broadcast(Message message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            for (WebSocket conn : connections) {
                if (conn.isOpen()) {
                    conn.send(jsonMessage);
                }
            }
        } catch (Exception e) {
            System.err.println("Error broadcasting message: " + e.getMessage());
        }
    }

    public int getConnectionCount() {
        return connections.size();
    }

    Message processIncomingRawJson(String rawJson) throws java.io.IOException {
        Message msg = objectMapper.readValue(rawJson, Message.class);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }
}
