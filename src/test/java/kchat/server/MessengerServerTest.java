package kchat.server;

import kchat.model.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MessengerServerTest {

    private MessengerServer server;
    private final int TEST_PORT = 8081; // Use different port to avoid conflicts

    @BeforeEach
    void setUp() {
        server = new MessengerServer(TEST_PORT);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                System.err.println("Error stopping test server: " + e.getMessage());
            }
        }
    }

    @Test
    void testServerCreation() {
        assertNotNull(server);
        assertEquals(0, server.getConnectionCount());
    }

    @Test
    void testServerStartAndStop() throws Exception {
        // Start server
        server.start();

        // Wait a bit for server to start
        Thread.sleep(100);

        // Server should be running
        assertTrue(server.getAddress() != null);

        // Stop server
        server.stop();

        // Wait a bit for server to stop
        Thread.sleep(100);
    }

    @Test
    void testConnectionCount() {
        assertEquals(0, server.getConnectionCount());
        // Note: Testing actual connections would require WebSocket client setup
        // which is more complex and might be better suited for integration tests
    }

    @Test
    void testPortConfiguration() {
        MessengerServer customPortServer = new MessengerServer(9090);
        assertEquals(9090, customPortServer.getAddress().getPort());

        try {
            customPortServer.stop();
        } catch (Exception e) {
            // Server wasn't started, so stopping might throw exception - that's ok
        }
    }
}
