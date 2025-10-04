package kchat.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import kchat.model.Message;
import kchat.security.KeyExchangeUtil;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.*;

import java.net.ServerSocket;
import java.net.URI;
import java.security.KeyPair;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

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
        private boolean authenticated = false;

        TestClient(URI serverUri) {
            super(serverUri);
        }

        List<Message> getReceived() {
            return received;
        }

        boolean isAuthenticated() {
            return authenticated;
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
        }

        @Override
        public void onMessage(String message) {
            try {
                Message msg = MAPPER.readValue(message, Message.class);
                received.add(msg);

                // Check if this is a WELCOME message indicating successful authentication
                if ("System".equals(msg.getSender()) && msg.getContent().startsWith("WELCOME:")) {
                    authenticated = true;
                }
            } catch (Exception ignored) {
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
        }

        @Override
        public void onError(Exception ex) {
        }

        void performHandshake(String serverId) throws Exception {
            // Generate client key pair and send HELLO message
            KeyPair clientKeyPair = KeyExchangeUtil.generateKeyPair();
            String pubB64 = Base64.getEncoder().encodeToString(clientKeyPair.getPublic().getEncoded());
            Message hello = new Message("Client", "HELLO:" + serverId + ":" + pubB64, System.currentTimeMillis());
            send(MAPPER.writeValueAsString(hello));
        }

        boolean waitForAuthentication(long timeoutMillis) throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeoutMillis;
            while (System.currentTimeMillis() < deadline && !authenticated) {
                Thread.sleep(25);
            }
            return authenticated;
        }

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
                    try {
                        return Integer.parseInt(m.getContent().substring("USER_COUNT:".length()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return -1;
        }
    }

    private TestClient newAuthenticatedClient() throws Exception {
        TestClient c = new TestClient(new URI("ws://localhost:" + port));
        boolean connected = c.connectBlocking(2, TimeUnit.SECONDS);
        assertTrue(connected, "Client failed to connect");

        // Perform authentication handshake
        c.performHandshake(server.getServerId());
        boolean authenticated = c.waitForAuthentication(2000);
        assertTrue(authenticated, "Client failed to authenticate");

        return c;
    }

    @Test
    void userCountMessagesAndConnectionCountStayInSync() throws Exception {
        // Initially no authenticated connections
        assertEquals(0, server.getAuthenticatedConnectionCount());

        // First authenticated client
        TestClient c1 = newAuthenticatedClient();
        assertTrue(c1.waitForUserCount(1, 2000), "Did not receive USER_COUNT:1");
        assertEquals(1, server.getAuthenticatedConnectionCount());

        // Second authenticated client
        TestClient c2 = newAuthenticatedClient();
        assertTrue(c2.waitForUserCount(2, 2000) || c1.waitForUserCount(2, 2000), "No client observed USER_COUNT:2");
        assertEquals(2, server.getAuthenticatedConnectionCount());

        // Close second client
        c2.closeBlocking();
        Thread.sleep(120);
        assertTrue(c1.waitForUserCount(1, 2000), "Remaining client did not observe USER_COUNT:1 after close");
        assertEquals(1, server.getAuthenticatedConnectionCount());

        // Close first client
        c1.closeBlocking();
        Thread.sleep(120);
        assertEquals(0, server.getAuthenticatedConnectionCount());
    }

    @Test
    void unauthenticatedClientsDoNotAffectUserCount() throws Exception {
        // Connect without authentication
        TestClient unauthenticated = new TestClient(new URI("ws://localhost:" + port));
        boolean connected = unauthenticated.connectBlocking(2, TimeUnit.SECONDS);
        assertTrue(connected, "Client failed to connect");

        // Should have raw connection but no authenticated connection
        assertEquals(1, server.getConnectionCount());
        assertEquals(0, server.getAuthenticatedConnectionCount());

        // Now connect with authentication
        TestClient authenticated = newAuthenticatedClient();
        assertTrue(authenticated.waitForUserCount(1, 2000), "Did not receive USER_COUNT:1");

        // Should have 2 raw connections but only 1 authenticated
        assertEquals(2, server.getConnectionCount());
        assertEquals(1, server.getAuthenticatedConnectionCount());

        // Close unauthenticated - should not affect user count broadcast
        unauthenticated.closeBlocking();
        Thread.sleep(120);

        // User count should still be 1
        assertEquals(1, server.getAuthenticatedConnectionCount());
        assertEquals(1, authenticated.lastUserCount());

        authenticated.closeBlocking();
    }
}
