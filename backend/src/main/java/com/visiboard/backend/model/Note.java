package com.visiboard.backend.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.locationtech.jts.geom.Point;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notes")
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "firebase_id")
    private String firebaseId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private java.util.List<Comment> comments = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private java.util.List<NoteLike> likes = new java.util.ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @JsonIgnore
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point location;

    private LocalDateTime createdAt;
    private int likesCount;
    private int commentsCount;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Note() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFirebaseId() { return firebaseId; }
    public void setFirebaseId(String firebaseId) { this.firebaseId = firebaseId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public java.util.List<Comment> getComments() { return comments; }
    public void setComments(java.util.List<Comment> comments) { this.comments = comments; }

    public java.util.List<NoteLike> getLikes() { return likes; }
    public void setLikes(java.util.List<NoteLike> likes) { this.likes = likes; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    @JsonIgnore
    public Point getLocation() { return location; }
    public void setLocation(Point location) { this.location = location; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }

    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }
    
    // Transient getters for latitude and longitude (for JSON serialization)
    @com.fasterxml.jackson.annotation.JsonProperty("lat")
    public Double getLat() {
        return location != null ? location.getY() : null;
    }
    
    @com.fasterxml.jackson.annotation.JsonProperty("lng")
    public Double getLng() {
        return location != null ? location.getX() : null;
    }
    
    public void setLat(double lat) {
        double lng = (this.location != null) ? this.location.getX() : 0.0;
        this.location = new org.locationtech.jts.geom.GeometryFactory().createPoint(new org.locationtech.jts.geom.Coordinate(lng, lat));
    }
    
    public void setLng(double lng) {
        double lat = (this.location != null) ? this.location.getY() : 0.0;
        this.location = new org.locationtech.jts.geom.GeometryFactory().createPoint(new org.locationtech.jts.geom.Coordinate(lng, lat));
    }
    @Transient
    private java.util.List<String> likedByUsers = new java.util.ArrayList<>();

    public java.util.List<String> getLikedByUsers() { return likedByUsers; }
    public void setLikedByUsers(java.util.List<String> likedByUsers) { this.likedByUsers = likedByUsers; }

    @Column(columnDefinition = "TEXT")
    private String imageBase64;
    private Integer imageWidth;
    private Integer imageHeight;

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public Integer getImageWidth() { return imageWidth; }
    public void setImageWidth(Integer imageWidth) { this.imageWidth = imageWidth; }

    public Integer getImageHeight() { return imageHeight; }
    public void setImageHeight(Integer imageHeight) { this.imageHeight = imageHeight; }
}
