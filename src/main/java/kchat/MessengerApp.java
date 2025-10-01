package kchat;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import kchat.ui.MessengerWindow;
import kchat.server.MessengerServer;

public class MessengerApp extends Application {

    private MessengerServer server;
    private MessengerWindow messengerWindow;

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(true); // ensure FX thread exits when last window closes
        server = new MessengerServer(8080);
        server.start();
        messengerWindow = new MessengerWindow();
        messengerWindow.show(primaryStage);

        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Primary stage close requested (MessengerApp)");
            // Close client first so close frame reaches server before it stops
            if (messengerWindow != null) {
                messengerWindow.shutdown();
            }
            if (server != null) {
                try {
                    server.stop();
                } catch (Exception e) {
                    System.err.println("Error stopping server: " + e.getMessage());
                }
            }
            // Terminate JavaFX runtime
            Platform.exit();
            // Hard JVM exit to ensure non-daemon WebSocket threads do not keep process alive
            new Thread(() -> {
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                System.out.println("Forcing JVM exit");
                System.exit(0);
            }, "forced-exit-thread").start();
        });
    }

    @Override
    public void stop() {
        System.out.println("Application.stop() invoked (MessengerApp)");
        if (messengerWindow != null) {
            messengerWindow.shutdown();
        }
        if (server != null) {
            try { server.stop(); } catch (Exception e) { System.err.println("Error stopping server in stop(): " + e.getMessage()); }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
