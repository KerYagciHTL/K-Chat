package kchat;

import kchat.server.MessengerServer;
import java.util.Scanner;

public class ServerLauncher {
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

        MessengerServer server = new MessengerServer(port);
        server.start();

        System.out.println("Messenger server started on port " + port);
        System.out.println("Press Enter to stop the server...");

        try (Scanner scanner = new Scanner(System.in)) {
            scanner.nextLine();
            server.stop();
            System.out.println("Server stopped.");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            try {
                server.stop();
            } catch (Exception stopException) {
                System.err.println("Error stopping server: " + stopException.getMessage());
            }
        }
    }
}
