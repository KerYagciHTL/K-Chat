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
        CapturingServer() { super(12345);}
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
        assertEquals("Server", welcome.getSender());
        assertEquals("User joined the chat", welcome.getContent());
        assertTrue(System.currentTimeMillis() - welcome.getTimestamp() < 2000);
    }

    @Test
    void leaveMessageFactoryHasExpectedContentAndSender() {
        Message leave = server.createLeaveMessage();
        assertEquals("Server", leave.getSender());
        assertEquals("User left the chat", leave.getContent());
        assertTrue(System.currentTimeMillis() - leave.getTimestamp() < 2000);
    }

    @Test
    void processIncomingRawJsonOverwritesTimestamp() throws Exception {
        long clientTimestamp = 1L;
        String json = String.format("{\n  \"sender\": \"Alice\",\n  \"content\": \"Hello\",\n  \"timestamp\": %d\n}", clientTimestamp);
        Message processed = server.processIncomingRawJson(json);
        assertEquals("Alice", processed.getSender());
        assertEquals("Hello", processed.getContent());
        assertNotEquals(clientTimestamp, processed.getTimestamp());
        assertTrue(System.currentTimeMillis() - processed.getTimestamp() < 2000);
    }

    @Test
    void broadcastMethodIsInvokedWithOriginalMessageInstance() {
        Message m = new Message("Bob", "Message body", 0L);
        server.broadcast(m);
        assertEquals(1, server.getBroadcasts().size());
        Message captured = server.getBroadcasts().get(0);
        assertSame(m, captured);
    }
}
