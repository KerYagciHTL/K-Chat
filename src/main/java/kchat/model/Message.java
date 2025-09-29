package kchat.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Message {
    private String sender;
    private String content;
    private long timestamp;

    @JsonCreator
    public Message(@JsonProperty("sender") String sender,
                   @JsonProperty("content") String content,
                   @JsonProperty("timestamp") long timestamp) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s",
            new java.util.Date(timestamp).toString(),
            sender,
            content);
    }
}
