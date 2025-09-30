package kchat;

import javafx.application.Application;
import javafx.stage.Stage;
import kchat.ui.MessengerWindow;

public class ClientLauncher extends Application {

    @Override
    public void start(Stage primaryStage) {
        MessengerWindow messengerWindow = new MessengerWindow();
        messengerWindow.show(primaryStage);

        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Client closing...");
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
