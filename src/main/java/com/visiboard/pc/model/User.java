package com.visiboard.pc.model;

public class User {
    private String userId;
    private String username;
    private String email;
    private String displayName;
    private String photoUrl;
    private boolean isBanned;
    private long banExpiry;
    private boolean isRestricted;
    private long restrictionExpiry;
    private long createdAt; // Using long for timestamp from Firebase

    public User() {} // No-arg constructor

    public User(String userId, String username, String email, String displayName, String photoUrl, long createdAt) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    @com.fasterxml.jackson.annotation.JsonProperty("firebaseUid")
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    @com.fasterxml.jackson.annotation.JsonProperty("name")
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPhotoUrl() { return photoUrl; }
    @com.fasterxml.jackson.annotation.JsonProperty("profilePicUrl")
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public boolean isBanned() { return isBanned; }
    public void setBanned(boolean banned) { isBanned = banned; }
    public long getBanExpiry() { return banExpiry; }
    public void setBanExpiry(long banExpiry) { this.banExpiry = banExpiry; }
    public boolean isRestricted() { return isRestricted; }
    public void setRestricted(boolean restricted) { this.isRestricted = restricted; }
    public long getRestrictionExpiry() { return restrictionExpiry; }
    public void setRestrictionExpiry(long restrictionExpiry) { this.restrictionExpiry = restrictionExpiry; }
    public long getCreatedAt() { return createdAt; }

    // Compatibility methods for existing code
    public String getName() { return displayName; }
    public void setName(String name) { this.displayName = name; }
    
    public String getProfilePicUrl() { return photoUrl; } // map photoUrl to profilePicUrl
    public void setProfilePicUrl(String url) { this.photoUrl = url; }
    
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public String getFirebaseUid() { return userId; }
    public void setFirebaseUid(String uid) { this.userId = uid; }

    public String getId() { return userId; }
    public void setId(String id) { this.userId = id; }

    
    // Stats fields (transient or loaded separately)
    private long totalLikesReceived;
    private long followersCount;
    private long followingCount;

    public long getTotalLikesReceived() { return totalLikesReceived; }
    public void setTotalLikesReceived(long totalLikesReceived) { this.totalLikesReceived = totalLikesReceived; }
    
    public long getFollowersCount() { return followersCount; }
    public void setFollowersCount(long followersCount) { this.followersCount = followersCount; }
    
    public long getFollowingCount() { return followingCount; }
    public void setFollowingCount(long followingCount) { this.followingCount = followingCount; }

    @Override
    public String toString() {
        return displayName + " (" + email + ")";
    }
}
