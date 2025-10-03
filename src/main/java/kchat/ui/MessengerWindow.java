package kchat.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import kchat.MessengerClient;
import kchat.model.Message;
import kchat.security.CryptoUtils;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MessengerWindow {

    private MessengerClient client;
    private TextArea messageArea;
    private TextField messageInput;
    private TextField usernameField;
    private TextField serverIdField; // new field for server ID
    private Button sendButton;
    private Button connectButton;
    private Label statusLabel;
    private Label userCountLabel;
    private String currentUsername = "User";

    public void shutdown() {
        if (client != null && !client.isClosed()) {
            try {
                client.closeBlocking();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void show(Stage stage) {
        VBox root = createUI();
        Scene scene = new Scene(root, 640, 520);
        stage.setTitle("Simple Messenger");
        stage.setScene(scene);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (client != null && !client.isClosed()) {
                try { client.closeBlocking(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }));
        stage.show();
        // Removed auto-connect: user must supply server ID and click Connect
        updateConnectionStatus("Disconnected");
    }

    private VBox createUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        VBox topPanel = createTopPanel();
        messageArea = new TextArea();
        messageArea.setEditable(false);
        messageArea.setWrapText(true);
        messageArea.setPrefRowCount(20);
        VBox.setVgrow(messageArea, Priority.ALWAYS);
        HBox inputPanel = createInputPanel();
        root.getChildren().addAll(topPanel, messageArea, inputPanel);
        return root;
    }

    private VBox createTopPanel() {
        VBox topPanel = new VBox(5);
        HBox connectionPanel = createConnectionPanel();
        HBox statusPanel = new HBox();
        statusPanel.setAlignment(Pos.CENTER_LEFT);
        statusLabel = new Label("Disconnected");
        statusLabel.setStyle("-fx-text-fill: red;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        userCountLabel = new Label("Users: 0");
        userCountLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2196F3;");
        statusPanel.getChildren().addAll(statusLabel, spacer, userCountLabel);
        topPanel.getChildren().addAll(connectionPanel, statusPanel);
        return topPanel;
    }

    private HBox createConnectionPanel() {
        HBox panel = new HBox(10);
        panel.setAlignment(Pos.CENTER_LEFT);
        Label usernameLabel = new Label("Username:");
        usernameField = new TextField(currentUsername);
        usernameField.setPrefWidth(120);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label serverIdLabel = new Label("Server ID:");
        serverIdField = new TextField();
        serverIdField.setPromptText("Paste server id");
        serverIdField.setPrefWidth(240);
        connectButton = new Button("Connect");
        connectButton.setOnAction(e -> connectToServer());
        panel.getChildren().addAll(usernameLabel, usernameField, spacer, serverIdLabel, serverIdField, connectButton);
        return panel;
    }

    private HBox createInputPanel() {
        HBox panel = new HBox(10);
        panel.setAlignment(Pos.CENTER);
        messageInput = new TextField();
        messageInput.setPromptText("Type your message here...");
        messageInput.setPrefWidth(400);
        messageInput.setOnAction(e -> sendMessage());
        sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage());
        sendButton.setDefaultButton(true);
        messageInput.setDisable(true);
        sendButton.setDisable(true);
        HBox.setHgrow(messageInput, Priority.ALWAYS);
        panel.getChildren().addAll(messageInput, sendButton);
        return panel;
    }

    private void connectToServer() {
        try {
            String enteredServerId = serverIdField.getText() == null ? "" : serverIdField.getText().trim();
            if (enteredServerId.isEmpty()) {
                updateConnectionStatus("Error: serverId required");
                return;
            }
            currentUsername = usernameField.getText().trim();
            if (currentUsername.isEmpty()) {
                currentUsername = "User";
                usernameField.setText(currentUsername);
            }
            if (client != null && !client.isClosed()) {
                try { client.close(); } catch (Exception ignore) {}
            }
            int port = 8080;
            if (!isServerReachable("localhost", port, 700)) {
                updateConnectionStatus("Error: server not reachable on port " + port);
                return;
            }
            String scheme = Boolean.getBoolean("kchat.ssl") ? "wss" : "ws";
            URI serverUri = new URI(scheme + "://localhost:" + port);
            client = new MessengerClient(serverUri);
            client.setTargetServerId(enteredServerId);
            client.setMessageHandler(this::handleIncomingMessage);
            client.setConnectionStatusHandler(this::updateConnectionStatus);
            client.connect();
            updateConnectionStatus("Connecting (socket)...");
        } catch (Exception e) {
            updateConnectionStatus("Connection failed: " + e.getMessage());
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    private boolean isServerReachable(String host, int port, int timeoutMs) {
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void sendMessage() {
        if (client == null || !client.isOpen()) {
            appendMessage("Error: Not connected to server");
            return;
        }
        String message = messageInput.getText().trim();
        if (message.isEmpty()) {
            return;
        }
        client.sendMessage(currentUsername, message);
        messageInput.clear();
        messageInput.requestFocus();
    }

    private void handleIncomingMessage(Message message) {
        Platform.runLater(() -> {
            if ("System".equals(message.getSender()) && message.getContent().startsWith("USER_COUNT:")) {
                String countStr = message.getContent().substring("USER_COUNT:".length());
                try {
                    int userCount = Integer.parseInt(countStr);
                    userCountLabel.setText("Users: " + userCount);
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing user count: " + countStr);
                }
                return;
            }
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            String timestamp = timeFormat.format(new Date(message.getTimestamp()));
            String formattedMessage = String.format("[%s] %s: %s", timestamp, message.getSender(), message.getContent());
            appendMessage(formattedMessage);
        });
    }

    private void updateConnectionStatus(String status) {
        Platform.runLater(() -> {
            statusLabel.setText("Status: " + status);
            boolean connected = status.startsWith("Connected");
            boolean working = status.startsWith("Connecting") || status.contains("Handshake");
            boolean error = status.startsWith("Error");
            if (connected) {
                statusLabel.setStyle("-fx-text-fill: green;");
                messageInput.setDisable(false);
                sendButton.setDisable(false);
                connectButton.setText("Reconnect");
                serverIdField.setStyle("");
            } else if (working) {
                statusLabel.setStyle("-fx-text-fill: orange;");
                messageInput.setDisable(true);
                sendButton.setDisable(true);
                connectButton.setText("Connect");
                serverIdField.setStyle("");
            } else if (error) {
                statusLabel.setStyle("-fx-text-fill: #d32f2f;");
                messageInput.setDisable(true);
                sendButton.setDisable(true);
                connectButton.setText("Connect");
                // Clear any previous connection status or user info
                // If you have a label or field showing 'User connected to the Server', clear it here
                // For example:
                // connectedUserLabel.setText("");
                // Optionally reset other UI elements as needed
                serverIdField.setStyle("-fx-border-color: #d32f2f;");
            }
        });
    }

    private void appendMessage(String message) {
        Platform.runLater(() -> {
            messageArea.appendText(message + "\n");
            messageArea.setScrollTop(Double.MAX_VALUE);
        });
    }
}
