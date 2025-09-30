package kchat;

import kchat.model.Message;
import kchat.server.MessengerServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class MessengerIntegrationTest {

    private MessengerServer server;
    private final int TEST_PORT = 8082;

    @BeforeEach
    void setUp() {
        server = new MessengerServer(TEST_PORT);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            try {
                server.stop();
                Thread.sleep(100);
            } catch (Exception e) {
                System.err.println("Error stopping test server: " + e.getMessage());
            }
        }
    }

    @Test
    void testServerLifecycle() throws Exception {
        assertDoesNotThrow(() -> {
            server.start();
            Thread.sleep(200);

            assertTrue(server.getAddress() != null);
            assertEquals(TEST_PORT, server.getAddress().getPort());

            server.stop();
            Thread.sleep(200);
        });
    }

    @Test
    void testMessageCreationAndSerialization() throws Exception {
        Message message = new Message("TestUser", "Hello Integration Test!", System.currentTimeMillis());

        assertNotNull(message);
        assertEquals("TestUser", message.getSender());
        assertEquals("Hello Integration Test!", message.getContent());

        String messageString = message.toString();
        assertTrue(messageString.contains("TestUser"));
        assertTrue(messageString.contains("Hello Integration Test!"));
    }

    @Test
    void testMultipleServerInstances() throws Exception {
        MessengerServer server2 = new MessengerServer(TEST_PORT + 1);
        MessengerServer server3 = new MessengerServer(TEST_PORT + 2);

        try {
            assertNotNull(server2);
            assertNotNull(server3);

            assertEquals(TEST_PORT, server.getAddress().getPort());
            assertEquals(TEST_PORT + 1, server2.getAddress().getPort());
            assertEquals(TEST_PORT + 2, server3.getAddress().getPort());

        } finally {
            try { if (server2 != null) server2.stop(); } catch (Exception e) { /* ignore */ }
            try { if (server3 != null) server3.stop(); } catch (Exception e) { /* ignore */ }
        }
    }
}
