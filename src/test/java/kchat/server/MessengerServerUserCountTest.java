package kchat.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import kchat.model.Message;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.*;

import java.net.ServerSocket;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deterministic test focusing on user count increment/decrement and USER_COUNT system messages.
 */
public class MessengerServerUserCountTest {

    private MessengerServer server;
    private int port;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static int findFreePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        port = findFreePort();
        server = new MessengerServer(port);
        server.start();
        Thread.sleep(150);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            server.stop();
            Thread.sleep(80);
        }
    }

    private static class TestClient extends WebSocketClient {
        private final List<Message> received = new CopyOnWriteArrayList<>();
        TestClient(URI serverUri) { super(serverUri); }
        List<Message> getReceived() { return received; }
        @Override public void onOpen(ServerHandshake handshakedata) { }
        @Override public void onMessage(String message) { try { received.add(MAPPER.readValue(message, Message.class)); } catch (Exception ignored) { } }
        @Override public void onClose(int code, String reason, boolean remote) { }
        @Override public void onError(Exception ex) { }
        boolean waitForUserCount(int expected, long timeoutMillis) throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeoutMillis;
            while (System.currentTimeMillis() < deadline) {
                int last = lastUserCount();
                if (last == expected) return true;
                Thread.sleep(25);
            }
            return lastUserCount() == expected;
        }
        int lastUserCount() {
            for (int i = received.size() - 1; i >= 0; i--) {
                Message m = received.get(i);
                if ("System".equals(m.getSender()) && m.getContent().startsWith("USER_COUNT:")) {
                    try { return Integer.parseInt(m.getContent().substring("USER_COUNT:".length())); } catch (NumberFormatException ignored) { }
                }
            }
            return -1;
        }
    }

    private TestClient newClient() throws Exception {
        TestClient c = new TestClient(new URI("ws://localhost:" + port));
        boolean connected = c.connectBlocking(2, TimeUnit.SECONDS);
        assertTrue(connected, "Client failed to connect");
        return c;
    }

    @Test
    void userCountMessagesAndConnectionCountStayInSync() throws Exception {
        assertEquals(0, server.getConnectionCount());
        TestClient c1 = newClient();
        assertTrue(c1.waitForUserCount(1, 2000), "Did not receive USER_COUNT:1");
        assertEquals(1, server.getConnectionCount());

        TestClient c2 = newClient();
        assertTrue(c2.waitForUserCount(2, 2000) || c1.waitForUserCount(2, 2000), "No client observed USER_COUNT:2");
        assertEquals(2, server.getConnectionCount());

        // Close second client
        c2.closeBlocking();
        Thread.sleep(120);
        // Remaining client should eventually see count 1
        assertTrue(c1.waitForUserCount(1, 2000), "Remaining client did not observe USER_COUNT:1 after close");
        assertEquals(1, server.getConnectionCount());

        // Close first client
        c1.closeBlocking();
        Thread.sleep(120);
        assertEquals(0, server.getConnectionCount());
    }
}

