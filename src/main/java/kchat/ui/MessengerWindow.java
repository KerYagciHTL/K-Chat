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
    private String currentUsername = "User";

    public void show(Stage stage) {
        VBox root = createUI();
        Scene scene = new Scene(root, 600, 500);

        stage.setTitle("Simple Messenger");
        stage.setScene(scene);
        stage.show();

        connectToServer();
    }

    private VBox createUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        HBox connectionPanel = createConnectionPanel();

        statusLabel = new Label("Disconnected");
        statusLabel.setStyle("-fx-text-fill: red;");

        messageArea = new TextArea();
        messageArea.setEditable(false);
        messageArea.setWrapText(true);
        messageArea.setPrefRowCount(20);
        VBox.setVgrow(messageArea, Priority.ALWAYS);

        HBox inputPanel = createInputPanel();

        root.getChildren().addAll(connectionPanel, statusLabel, messageArea, inputPanel);
        return root;
    }

    private HBox createConnectionPanel() {
        HBox panel = new HBox(10);
        panel.setAlignment(Pos.CENTER_LEFT);

        Label usernameLabel = new Label("Username:");
        usernameField = new TextField(currentUsername);
        usernameField.setPrefWidth(150);

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

            if (client != null && !client.isClosed()) {
                client.close();
            }

            URI serverUri = new URI("ws://localhost:8080");
            client = new MessengerClient(serverUri);

            client.setMessageHandler(this::handleIncomingMessage);

            client.setConnectionStatusHandler(this::updateConnectionStatus);

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
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            String timestamp = timeFormat.format(new Date(message.getTimestamp()));
            String formattedMessage = String.format("[%s] %s: %s",
                timestamp, message.getSender(), message.getContent());
            appendMessage(formattedMessage);
        });
    }

    private void updateConnectionStatus(String status) {
        Platform.runLater(() -> {
            statusLabel.setText("Status: " + status);

            if (status.equals("Connected")) {
                statusLabel.setStyle("-fx-text-fill: green;");
                messageInput.setDisable(false);
                sendButton.setDisable(false);
                connectButton.setText("Reconnect");
                messageInput.requestFocus();
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
            messageArea.setScrollTop(Double.MAX_VALUE); // Auto-scroll to bottom
        });
    }
}
