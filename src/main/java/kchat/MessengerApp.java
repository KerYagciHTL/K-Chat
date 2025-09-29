package kchat;

import javafx.application.Application;
import javafx.stage.Stage;
import kchat.ui.MessengerWindow;
import kchat.server.MessengerServer;

public class MessengerApp extends Application {

    private MessengerServer server;

    @Override
    public void start(Stage primaryStage) {
        server = new MessengerServer(8080);
        server.start();
        MessengerWindow messengerWindow = new MessengerWindow();
        messengerWindow.show(primaryStage);

        primaryStage.setOnCloseRequest(event -> {
            if (server != null) {
                try {
                    server.stop();
                } catch (Exception e) {
                    System.err.println("Error stopping server: " + e.getMessage());
                }
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
