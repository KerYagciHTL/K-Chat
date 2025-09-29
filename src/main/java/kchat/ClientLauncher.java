package kchat;

import javafx.application.Application;
import javafx.stage.Stage;
import kchat.ui.MessengerWindow;

public class ClientLauncher extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Only start the GUI client - no server
        MessengerWindow messengerWindow = new MessengerWindow();
        messengerWindow.show(primaryStage);

        // No server cleanup needed since we don't start one
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Client closing...");
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
