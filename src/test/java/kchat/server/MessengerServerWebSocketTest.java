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

@Disabled("Flaky network timing; replaced by deterministic MessengerServerLogicTest")
public class MessengerServerWebSocketTest {

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
        Thread.sleep(200);
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
        @Override public void onError(Exception ex) { /* swallow to let retries work */ }
        boolean waitForAtLeast(int expected, long timeoutMillis) throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeoutMillis;
            while (System.currentTimeMillis() < deadline) {
                if (received.size() >= expected) return true;
                Thread.sleep(20);
            }
            return received.size() >= expected;
        }
    }

    private TestClient newClient() throws Exception {
        int attempts = 0;
        Exception last = null;
        while (attempts < 6) {
            TestClient c = new TestClient(new URI("ws://localhost:" + port));
            boolean connected = c.connectBlocking(2, TimeUnit.SECONDS);
            if (connected) return c;
            last = new Exception("connect attempt " + (attempts + 1) + " failed");
            try { c.close(); } catch (Exception ignore) { }
            Thread.sleep(100L * (attempts + 1));
            attempts++;
        }
        fail("Failed to connect client after retries: " + (last != null ? last.getMessage() : "unknown"));
        return null; // unreachable
    }

    @Test
    void connectionCountIncrementsAndDecrements() throws Exception {
        assertEquals(0, server.getConnectionCount());
        TestClient c1 = newClient();
        long deadline = System.currentTimeMillis() + 2000;
        while (server.getConnectionCount() < 1 && System.currentTimeMillis() < deadline) Thread.sleep(25);
        assertEquals(1, server.getConnectionCount());
        c1.closeBlocking();
        Thread.sleep(150); // allow onClose
        assertEquals(0, server.getConnectionCount());
    }

    @Test
    void userMessageBroadcastsToAllConnectedClientsAndTimestampIsServerAssigned() throws Exception {
        TestClient sender = newClient();
        TestClient receiver = newClient();

        long deadline = System.currentTimeMillis() + 2500;
        while (server.getConnectionCount() < 2 && System.currentTimeMillis() < deadline) Thread.sleep(25);
        assertEquals(2, server.getConnectionCount(), "Both clients not registered in time");

        sender.getReceived().clear();
        receiver.getReceived().clear();

        Message outgoing = new Message("Alice", "Hello everyone", 0L); // timestamp should be overwritten
        String json = MAPPER.writeValueAsString(outgoing);
        sender.send(json);

        assertTrue(sender.waitForAtLeast(1, 3000), "Sender did not receive broadcast");
        assertTrue(receiver.waitForAtLeast(1, 3000), "Receiver did not receive broadcast");

        Message senderView = sender.getReceived().stream().filter(m -> "Hello everyone".equals(m.getContent())).findFirst()
            .orElseThrow(() -> new AssertionError("Sender missing user message"));
        Message receiverView = receiver.getReceived().stream().filter(m -> "Hello everyone".equals(m.getContent())).findFirst()
            .orElseThrow(() -> new AssertionError("Receiver missing user message"));

        assertEquals("Alice", senderView.getSender());
        assertEquals("Alice", receiverView.getSender());

        long now = System.currentTimeMillis();
        assertTrue(senderView.getTimestamp() > 0, "Timestamp not set by server");
        assertTrue(now - senderView.getTimestamp() < 8000, "Timestamp too old");
        assertEquals(senderView.getTimestamp(), receiverView.getTimestamp(), "Broadcast timestamps differ");

        sender.closeBlocking();
        receiver.closeBlocking();
    }
}
