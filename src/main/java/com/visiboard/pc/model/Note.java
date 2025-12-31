package com.visiboard.pc.model;

public class Note {
    private String noteId;
    private String userId;
    private String content;
    private String imageUrl;
    private double latitude;
    private double longitude;
    private int likesCount;
    private boolean isHidden;
    private long createdAt;

    public Note() {} // No-arg constructor

    public String getNoteId() { return noteId; }
    @com.fasterxml.jackson.annotation.JsonProperty("firebaseId")
    public void setNoteId(String noteId) { this.noteId = noteId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageUrl() { return imageUrl; }
    @com.fasterxml.jackson.annotation.JsonProperty("imageBase64")
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }

    public boolean isHidden() { return isHidden; }
    public void setHidden(boolean hidden) { isHidden = hidden; }

    public long getCreatedAt() { return createdAt; }
    
    @com.fasterxml.jackson.annotation.JsonProperty("createdAt")
    public void setCreatedAt(Object timestamp) {
        if (timestamp instanceof Number) {
            this.createdAt = ((Number) timestamp).longValue();
        } else if (timestamp instanceof String) {
            try {
                this.createdAt = java.time.LocalDateTime.parse((String) timestamp).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (Exception e) {
                this.createdAt = System.currentTimeMillis();
            }
        } else if (timestamp instanceof java.util.List) {
            // Handle array [yyyy, MM, dd, HH, mm, ss]
            try {
                java.util.List<Integer> t = (java.util.List<Integer>) timestamp;
                java.time.LocalDateTime ldt = java.time.LocalDateTime.of(
                    t.get(0), t.get(1), t.get(2), 
                    t.size() > 3 ? t.get(3) : 0, 
                    t.size() > 4 ? t.get(4) : 0, 
                    t.size() > 5 ? t.get(5) : 0
                );
                this.createdAt = ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (Exception e) {
                this.createdAt = System.currentTimeMillis();
            }
        }
    }

    // Compatibility methods
    public String getId() { return noteId; }
    // public String getImageBase64() { return imageUrl; } // Removed to prevent Jackson conflict
    public Double getLat() { return latitude; }
    public Double getLng() { return longitude; }
    
    public void setLat(Double lat) { this.latitude = lat; }
    public void setLng(Double lng) { this.longitude = lng; }

    private User user;
    private long commentsCount;

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public long getCommentsCount() { return commentsCount; }
    public void setCommentsCount(long commentsCount) { this.commentsCount = commentsCount; }

    private java.util.List<String> likedByUsers;
    public java.util.List<String> getLikedByUsers() { return likedByUsers; }
    public void setLikedByUsers(java.util.List<String> likedByUsers) { this.likedByUsers = likedByUsers; }
}
