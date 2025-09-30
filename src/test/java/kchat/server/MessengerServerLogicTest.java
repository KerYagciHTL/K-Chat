package kchat.server;

import kchat.model.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MessengerServerLogicTest {

    private static class CapturingServer extends MessengerServer {
        private final List<Message> broadcasts = new ArrayList<>();
        CapturingServer() { super(12345); /* arbitrary unused port (won't be started) */ }
        @Override
        protected void broadcast(Message message) {
            broadcasts.add(message);
        }
        List<Message> getBroadcasts() { return broadcasts; }
    }

    private CapturingServer server;

    @BeforeEach
    void setUp() {
        server = new CapturingServer();
    }

    @Test
    void welcomeMessageFactoryHasExpectedContentAndSender() {
        Message welcome = server.createWelcomeMessage();
        assertEquals("Server", welcome.getSender(), "Welcome message sender changed");
        assertEquals("User joined the chat", welcome.getContent(), "Welcome message content changed");
        assertTrue(System.currentTimeMillis() - welcome.getTimestamp() < 2000, "Welcome timestamp not current");
    }

    @Test
    void leaveMessageFactoryHasExpectedContentAndSender() {
        Message leave = server.createLeaveMessage();
        assertEquals("Server", leave.getSender(), "Leave message sender changed");
        assertEquals("User left the chat", leave.getContent(), "Leave message content changed");
        assertTrue(System.currentTimeMillis() - leave.getTimestamp() < 2000, "Leave timestamp not current");
    }

    @Test
    void processIncomingRawJsonOverwritesTimestamp() throws Exception {
        long clientTimestamp = 1L; // unrealistic old timestamp
        String json = String.format("{\n  \"sender\": \"Alice\",\n  \"content\": \"Hello\",\n  \"timestamp\": %d\n}", clientTimestamp);
        Message processed = server.processIncomingRawJson(json);
        assertEquals("Alice", processed.getSender());
        assertEquals("Hello", processed.getContent());
        assertNotEquals(clientTimestamp, processed.getTimestamp(), "Server failed to overwrite client timestamp");
        assertTrue(System.currentTimeMillis() - processed.getTimestamp() < 2000, "Processed timestamp not current");
    }

    @Test
    void broadcastMethodIsInvokedWithOriginalMessageInstance() {
        Message m = new Message("Bob", "Message body", 0L);
        server.broadcast(m);
        assertEquals(1, server.getBroadcasts().size());
        Message captured = server.getBroadcasts().get(0);
        assertSame(m, captured, "Broadcast should use same message instance (serialization happens inside method)");
    }
}

