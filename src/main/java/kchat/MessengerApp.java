package kchat;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import kchat.ui.MessengerWindow;
import kchat.server.MessengerServer;
import java.net.ServerSocket;

public class MessengerApp extends Application {

    private MessengerServer server;
    private MessengerWindow messengerWindow;
    private static final int DEFAULT_PORT = 8080;

    private static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(true); // ensure FX thread exits when last window closes
        int port = DEFAULT_PORT;

        boolean startEmbeddedServer = isPortAvailable(port);
        if (!startEmbeddedServer) {
            System.out.println("Detected existing server on port " + port + ". Will NOT start a new embedded server; launching client only.");
        }

        if (startEmbeddedServer) {
            server = new MessengerServer(port);
            try {
                server.start();
                System.out.println("Embedded server started on port " + port);
            } catch (Exception e) {
                System.err.println("Failed to start embedded server: " + e.getMessage());
                server = null; // fallback to client-only
            }
        }

        messengerWindow = new MessengerWindow();
        messengerWindow.show(primaryStage);

        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Primary stage close requested (MessengerApp)");
            if (messengerWindow != null) {
                messengerWindow.shutdown();
            }
            if (server != null) {
                try { server.stop(); } catch (Exception e) { System.err.println("Error stopping server: " + e.getMessage()); }
            }
            Platform.exit();
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
