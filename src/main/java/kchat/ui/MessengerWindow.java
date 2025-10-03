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
        Scene scene = new Scene(root, 600, 520);
        stage.setTitle("Simple Messenger");
        stage.setScene(scene);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (client != null && !client.isClosed()) {
                try { client.closeBlocking(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }));
        stage.show();
        connectToServer();
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
        connectButton = new Button("Connect");
        connectButton.setOnAction(e -> connectToServer());
        panel.getChildren().addAll(usernameLabel, usernameField, connectButton);
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
            currentUsername = usernameField.getText().trim();
            if (currentUsername.isEmpty()) {
                currentUsername = "User";
                usernameField.setText(currentUsername);
            }
            String secret = System.getProperty("kchat.secret");
            if (secret != null && !secret.isEmpty()) {
                CryptoUtils.setPassphrase(secret);
            } else {
                CryptoUtils.clearPassphrase();
            }
            if (client != null && !client.isClosed()) {
                client.close();
            }
            String scheme = Boolean.getBoolean("kchat.ssl") ? "wss" : "ws";
            URI serverUri = new URI(scheme + "://localhost:8080");
            client = new MessengerClient(serverUri);
            client.setMessageHandler(this::handleIncomingMessage);
            client.setConnectionStatusHandler(this::updateConnectionStatus);
            client.setEncryptionEnabled(CryptoUtils.isEnabled());
            client.connect();
            updateConnectionStatus("Connecting...");
        } catch (Exception e) {
            updateConnectionStatus("Connection failed: " + e.getMessage());
            System.err.println("Connection error: " + e.getMessage());
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
            if (status.startsWith("Connected")) {
                statusLabel.setStyle("-fx-text-fill: green;");
                messageInput.setDisable(false);
                sendButton.setDisable(false);
                connectButton.setText("Reconnect");
                messageInput.requestFocus();
            } else if (status.startsWith("Connecting")) {
                statusLabel.setStyle("-fx-text-fill: orange;");
                messageInput.setDisable(true);
                sendButton.setDisable(true);
                connectButton.setText("Connect");
            } else {
                statusLabel.setStyle("-fx-text-fill: red;");
                messageInput.setDisable(true);
                sendButton.setDisable(true);
                connectButton.setText("Connect");
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
