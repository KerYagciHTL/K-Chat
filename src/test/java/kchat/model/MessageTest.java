package kchat.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class MessageTest {

    private ObjectMapper objectMapper;
    private Message message;
    private final String testSender = "TestUser";
    private final String testContent = "Hello, World!";
    private final long testTimestamp = System.currentTimeMillis();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        message = new Message(testSender, testContent, testTimestamp);
    }

    @Test
    void testMessageCreation() {
        assertNotNull(message);
        assertEquals(testSender, message.getSender());
        assertEquals(testContent, message.getContent());
        assertEquals(testTimestamp, message.getTimestamp());
    }

    @Test
    void testGettersAndSetters() {
        // Test setters
        message.setSender("NewUser");
        message.setContent("New message content");
        message.setTimestamp(12345L);

        // Test getters
        assertEquals("NewUser", message.getSender());
        assertEquals("New message content", message.getContent());
        assertEquals(12345L, message.getTimestamp());
    }

    @Test
    void testToString() {
        String result = message.toString();
        assertNotNull(result);
        assertTrue(result.contains(testSender));
        assertTrue(result.contains(testContent));
        // Should contain formatted timestamp
        assertTrue(result.matches("\\[.*\\] " + testSender + ": " + testContent));
    }

    @Test
    void testJsonSerialization() throws Exception {
        // Test serialization to JSON
        String json = objectMapper.writeValueAsString(message);
        assertNotNull(json);
        assertTrue(json.contains("\"sender\":\"" + testSender + "\""));
        assertTrue(json.contains("\"content\":\"" + testContent + "\""));
        assertTrue(json.contains("\"timestamp\":" + testTimestamp));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        // Test deserialization from JSON
        String json = String.format(
            "{\"sender\":\"%s\",\"content\":\"%s\",\"timestamp\":%d}",
            testSender, testContent, testTimestamp
        );

        Message deserializedMessage = objectMapper.readValue(json, Message.class);

        assertNotNull(deserializedMessage);
        assertEquals(testSender, deserializedMessage.getSender());
        assertEquals(testContent, deserializedMessage.getContent());
        assertEquals(testTimestamp, deserializedMessage.getTimestamp());
    }

    @Test
    void testJsonRoundTrip() throws Exception {
        // Test complete serialization/deserialization cycle
        String json = objectMapper.writeValueAsString(message);
        Message roundTripMessage = objectMapper.readValue(json, Message.class);

        assertEquals(message.getSender(), roundTripMessage.getSender());
        assertEquals(message.getContent(), roundTripMessage.getContent());
        assertEquals(message.getTimestamp(), roundTripMessage.getTimestamp());
    }

    @Test
    void testEmptyValues() {
        Message emptyMessage = new Message("", "", 0L);
        assertEquals("", emptyMessage.getSender());
        assertEquals("", emptyMessage.getContent());
        assertEquals(0L, emptyMessage.getTimestamp());
    }

    @Test
    void testNullValues() {
        Message nullMessage = new Message(null, null, 0L);
        assertNull(nullMessage.getSender());
        assertNull(nullMessage.getContent());
        assertEquals(0L, nullMessage.getTimestamp());
    }
}
