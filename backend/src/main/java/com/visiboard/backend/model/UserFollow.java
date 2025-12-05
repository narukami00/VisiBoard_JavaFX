package com.visiboard.backend.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_follows", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"follower_id", "followed_id"})
})
public class UserFollow {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "followed_id", nullable = false)
    private User followed;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public UserFollow() {}

    public UserFollow(User follower, User followed) {
        this.follower = follower;
        this.followed = followed;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getFollower() { return follower; }
    public void setFollower(User follower) { this.follower = follower; }

    public User getFollowed() { return followed; }
    public void setFollowed(User followed) { this.followed = followed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
