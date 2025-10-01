package kchat;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import kchat.ui.MessengerWindow;

public class ClientLauncher extends Application {

    private MessengerWindow messengerWindow;

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(true);
        messengerWindow = new MessengerWindow();
        messengerWindow.show(primaryStage);

        primaryStage.setOnCloseRequest(event -> {
            if (messengerWindow != null) {
                messengerWindow.shutdown();
            }
            Platform.exit();
            new Thread(() -> {
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                System.exit(0);
            }, "forced-exit-thread-client").start();
        });
    }

    @Override
    public void stop() {
        if (messengerWindow != null) {
            messengerWindow.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
