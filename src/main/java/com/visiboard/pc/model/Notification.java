package com.visiboard.pc.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Notification {
    private String id;
    private String type; // "LIKE", "COMMENT", "FOLLOW"
    private String message;
    private boolean read;
    
    @JsonProperty("recipient")
    private User recipient;
    
    @JsonProperty("sender")
    private User sender;
    
    @JsonProperty("note")
    private Note note;
    
    @JsonProperty("createdAt")
    private String createdAt;

    public Notification() {
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public Note getNote() {
        return note;
    }

    public void setNote(Note note) {
        this.note = note;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    // Helper methods for backward compatibility
    public String getFromUserId() {
        return sender != null ? sender.getId().toString() : null;
    }

    public String getFromUserName() {
        return sender != null ? sender.getName() : "Unknown";
    }

    public String getNoteId() {
        return note != null ? note.getId().toString() : null;
    }

    public String getNoteText() {
        return note != null ? note.getContent() : null;
    }

    public double getNoteLat() {
        return note != null && note.getLat() != null ? note.getLat() : 0.0;
    }

    public double getNoteLng() {
        return note != null && note.getLng() != null ? note.getLng() : 0.0;
    }

    public String getDisplayMessage() {
        if (message != null && !message.isEmpty()) {
            return message;
        }

        String senderName = getFromUserName();
        switch (type.toUpperCase()) {
            case "LIKE":
                return senderName + " liked your note";
            case "COMMENT":
                return senderName + " commented on your note";
            case "FOLLOW":
                return senderName + " started following you";
            default:
                return "New notification from " + senderName;
        }
    }

    public String getTimeAgo() {
        if (createdAt == null) return "Unknown";
        
        try {
            java.time.LocalDateTime created = java.time.LocalDateTime.parse(createdAt);
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            long seconds = java.time.Duration.between(created, now).getSeconds();

            if (seconds < 60) return "Just now";
            if (seconds < 3600) return (seconds / 60) + "m ago";
            if (seconds < 86400) return (seconds / 3600) + "h ago";
            if (seconds < 604800) return (seconds / 86400) + "d ago";
            return (seconds / 604800) + "w ago";
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
