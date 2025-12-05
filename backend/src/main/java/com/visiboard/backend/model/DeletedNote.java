package com.visiboard.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deleted_notes")
public class DeletedNote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "firebase_doc_id")
    private String firebaseDocId;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @PrePersist
    protected void onCreate() {
        deletedAt = LocalDateTime.now();
    }
    
    public DeletedNote() {}
    
    public DeletedNote(String content, String firebaseDocId) {
        this.content = content;
        this.firebaseDocId = firebaseDocId;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getFirebaseDocId() { return firebaseDocId; }
    public void setFirebaseDocId(String firebaseDocId) { this.firebaseDocId = firebaseDocId; }
    
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
