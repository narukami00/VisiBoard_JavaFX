package com.visiboard.pc.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Note {
    private UUID id;
    private User user;
    private String content;
    private String summary;
    private Double lat;
    private Double lng;
    private int likesCount;
    private int commentsCount;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }

    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }
    
    private String createdAt;

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return content != null ? content : "Empty Note";
    }

    private java.util.List<String> likedByUsers;
    public java.util.List<String> getLikedByUsers() { return likedByUsers; }
    public void setLikedByUsers(java.util.List<String> likedByUsers) { this.likedByUsers = likedByUsers; }
}
