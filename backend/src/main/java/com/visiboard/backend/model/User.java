
package com.visiboard.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    private String password;
    
    @Column(unique = true)
    private String firebaseUid;

    @Column(columnDefinition = "TEXT")
    private String profilePicUrl;
    private String currentTier;

    @JsonIgnore
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point lastLocation;

    private int followersCount;
    private int followingCount;
    
    @Column(columnDefinition = "integer default 0", nullable = false)
    private int totalLikesReceived = 0;

    private java.time.LocalDateTime createdAt;

    public User() {}

    public User(String name, String email, String profilePicUrl) {
        this.name = name;
        this.email = email;
        this.profilePicUrl = profilePicUrl;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirebaseUid() { return firebaseUid; }
    public void setFirebaseUid(String firebaseUid) { this.firebaseUid = firebaseUid; }

    public String getProfilePicUrl() { return profilePicUrl; }
    public void setProfilePicUrl(String profilePicUrl) { this.profilePicUrl = profilePicUrl; }

    public String getCurrentTier() { return currentTier; }
    public void setCurrentTier(String currentTier) { this.currentTier = currentTier; }

    @JsonIgnore
    public Point getLastLocation() { return lastLocation; }
    public void setLastLocation(Point lastLocation) { this.lastLocation = lastLocation; }

    public int getFollowersCount() { return followersCount; }
    public void setFollowersCount(int followersCount) { this.followersCount = followersCount; }

    public int getFollowingCount() { return followingCount; }
    public void setFollowingCount(int followingCount) { this.followingCount = followingCount; }

    public int getTotalLikesReceived() { return totalLikesReceived; }
    public void setTotalLikesReceived(int totalLikesReceived) { this.totalLikesReceived = totalLikesReceived; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Transient getters for latitude and longitude (for JSON serialization)
    @com.fasterxml.jackson.annotation.JsonProperty("lat")
    public Double getLat() {
        return lastLocation != null ? lastLocation.getY() : null;
    }
    
    @com.fasterxml.jackson.annotation.JsonProperty("lng")
    public Double getLng() {
        return lastLocation != null ? lastLocation.getX() : null;
    }
}
