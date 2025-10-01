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
            System.out.println("Primary stage close requested (ClientLauncher)");
            if (messengerWindow != null) {
                messengerWindow.shutdown();
            }
            Platform.exit();
            new Thread(() -> {
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                System.out.println("Forcing JVM exit (client)");
                System.exit(0);
            }, "forced-exit-thread-client").start();
        });
    }

    @Override
    public void stop() {
        System.out.println("Application.stop() invoked (ClientLauncher)");
        if (messengerWindow != null) {
            messengerWindow.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
