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
    private final int TEST_PORT = 8081;

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
        server.start();

        Thread.sleep(100);

        assertTrue(server.getAddress() != null);

        server.stop();

        Thread.sleep(100);
    }

    @Test
    void testConnectionCount() {
        assertEquals(0, server.getConnectionCount());
    }

    @Test
    void testPortConfiguration() {
        MessengerServer customPortServer = new MessengerServer(9090);
        assertEquals(9090, customPortServer.getAddress().getPort());

        try {
            customPortServer.stop();
        } catch (Exception e) {
        }
    }
}
