package kchat;

import kchat.server.MessengerServer;
import java.util.Scanner;
import java.net.ServerSocket;

public class ServerLauncher {
    private static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) {
        int port = 8080;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                System.err.println("Using default port: " + port);
            }
        }

        if (!isPortAvailable(port)) {
            System.err.println("A server already appears to be running on port " + port + ". Aborting new server start.");
            System.err.println("(If this is unexpected, ensure previous process is terminated.)");
            return; // Do not start another server
        }

        MessengerServer server = new MessengerServer(port);
        try {
            server.start();
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            return;
        }

        System.out.println("Messenger server started on port " + port);
        System.out.println("Press Enter to stop the server...");

        try (Scanner scanner = new Scanner(System.in)) {
            scanner.nextLine();
            server.stop();
            System.out.println("Server stopped.");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            try { server.stop(); } catch (Exception stopException) { System.err.println("Error stopping server: " + stopException.getMessage()); }
        }
    }
}
