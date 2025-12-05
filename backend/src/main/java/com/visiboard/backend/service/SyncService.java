package com.visiboard.backend.service;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.visiboard.backend.model.Note;
import com.visiboard.backend.model.User;
import com.visiboard.backend.repository.NoteRepository;
import com.visiboard.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class SyncService {

    private final UserRepository userRepository;
    private final NoteRepository noteRepository;
    private final com.visiboard.backend.repository.NoteLikeRepository noteLikeRepository;
    private final com.visiboard.backend.repository.CommentRepository commentRepository;
    private final com.visiboard.backend.repository.NotificationRepository notificationRepository;
    private final com.visiboard.backend.repository.MessageRepository messageRepository;
    private final com.visiboard.backend.repository.UserFollowRepository userFollowRepository;

    public SyncService(UserRepository userRepository, 
                       NoteRepository noteRepository,
                       com.visiboard.backend.repository.NoteLikeRepository noteLikeRepository,
                       com.visiboard.backend.repository.CommentRepository commentRepository,
                       com.visiboard.backend.repository.NotificationRepository notificationRepository,
                       com.visiboard.backend.repository.MessageRepository messageRepository,
                       com.visiboard.backend.repository.UserFollowRepository userFollowRepository) {
        this.userRepository = userRepository;
        this.noteRepository = noteRepository;
        this.noteLikeRepository = noteLikeRepository;
        this.commentRepository = commentRepository;
        this.notificationRepository = notificationRepository;
        this.messageRepository = messageRepository;
        this.userFollowRepository = userFollowRepository;
    }

    public void syncUserToFirebase(User user) {
        Firestore db = FirestoreClient.getFirestore();
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", user.getEmail()); // Using email as username
        userData.put("email", user.getEmail());
        userData.put("displayName", user.getName());
        // Add other fields as needed

        try {
            db.collection("users").document(user.getFirebaseUid()).set(userData).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void syncNoteToFirebase(Note note) {
        Firestore db = FirestoreClient.getFirestore();
        Map<String, Object> noteData = new HashMap<>();
        noteData.put("content", note.getContent());
        noteData.put("latitude", note.getLat());
        noteData.put("longitude", note.getLng());
        noteData.put("userId", note.getUser().getFirebaseUid());
        // Add other fields

        try {
            db.collection("notes").document(note.getId().toString()).set(noteData).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void deleteNoteFromFirebase(String noteId) {
        Firestore db = FirestoreClient.getFirestore();
        try {
            db.collection("notes").document(noteId).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void syncFromFirebase() {
        try {
            messageRepository.deleteAll();
            userFollowRepository.deleteAll();
            noteRepository.deleteAll();
            userRepository.deleteAll();
            System.out.println("Local database cleared successfully.");
        } catch (Exception e) {
            System.err.println("Error clearing local database: " + e.getMessage());
            e.printStackTrace();
        }
        
        Firestore db = FirestoreClient.getFirestore();
        
        // Sync Users
        try {
            db.collection("users").get().get().getDocuments().forEach(document -> {
                User user = new User();
                user.setFirebaseUid(document.getId());
                user.setEmail(document.getString("email"));
                
                // Try 'name' first, then 'displayName', then 'username'
                String name = document.getString("name");
                if (name == null) name = document.getString("displayName");
                if (name == null) name = document.getString("username");
                user.setName(name);
                
                // Try 'profilePic' then 'profilePicUrl'
                String pic = document.getString("profilePic");
                if (pic == null) pic = document.getString("profilePicUrl");
                user.setProfilePicUrl(pic);
                
                userRepository.save(user);
            });
            System.out.println("Users synced from Firebase");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Sync Notes
        try {
            db.collection("notes").get().get().getDocuments().forEach(document -> {
                try {
                    Note note = new Note();
                    
                    // Content Mapping
                    String content = document.getString("note"); // Android uses "note"
                    if (content == null) content = document.getString("content");
                    if (content == null) content = document.getString("text");
                    if (content == null) content = "No Content";
                    note.setContent(content);
                    
                    // Location Mapping
                    Double lat = document.getDouble("lat");
                    Double lng = document.getDouble("lon");
                    if (lat == null) lat = document.getDouble("latitude");
                    if (lng == null) lng = document.getDouble("longitude");
                    
                    if (lat != null && lng != null) {
                        org.locationtech.jts.geom.GeometryFactory geometryFactory = new org.locationtech.jts.geom.GeometryFactory();
                        note.setLocation(geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(lng, lat)));
                    } else {
                        com.google.cloud.firestore.GeoPoint geoPoint = document.getGeoPoint("location");
                        if (geoPoint != null) {
                            org.locationtech.jts.geom.GeometryFactory geometryFactory = new org.locationtech.jts.geom.GeometryFactory();
                            note.setLocation(geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(geoPoint.getLongitude(), geoPoint.getLatitude())));
                        }
                    }
                    
                    // Timestamp Mapping
                    Object timestampObj = document.get("timestamp");
                    if (timestampObj instanceof com.google.cloud.Timestamp) {
                        note.setCreatedAt(java.time.LocalDateTime.ofInstant(
                            ((com.google.cloud.Timestamp) timestampObj).toDate().toInstant(), 
                            java.time.ZoneId.systemDefault()
                        ));
                    } else if (timestampObj instanceof Long) {
                        note.setCreatedAt(java.time.LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli((Long) timestampObj), 
                            java.time.ZoneId.systemDefault()
                        ));
                    } else {
                        note.setCreatedAt(java.time.LocalDateTime.now()); // Fallback
                    }
                    
                    // Stats Mapping
                    Long likes = document.getLong("likeCount");
                    note.setLikesCount(likes != null ? likes.intValue() : 0);
                    
                    Long comments = document.getLong("commentsCount");
                    note.setCommentsCount(comments != null ? comments.intValue() : 0);

                    // User Mapping
                    String userId = document.getString("userId");
                    if (userId != null) {
                        User user = userRepository.findByFirebaseUid(userId);
                        if (user != null) {
                            note.setUser(user);
                            noteRepository.save(note);
                        } else {
                            // Create temporary user if missing (Self-healing)
                            User tempUser = new User();
                            tempUser.setFirebaseUid(userId);
                            tempUser.setEmail("unknown@user.com");
                            tempUser.setName(document.getString("userName")); // Android saves userName in note
                            if (tempUser.getName() == null) tempUser.setName("Unknown User");
                            tempUser.setProfilePicUrl(document.getString("userProfilePic")); // Android saves pic in note
                            userRepository.save(tempUser);
                            
                            note.setUser(tempUser);
                            noteRepository.save(note);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error syncing note " + document.getId() + ": " + e.getMessage());
                }
            });
            System.out.println("Notes synced from Firebase");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Sync Comments from subcollections
        try {
            // Get all notes and check their comments subcollection
            db.collection("notes").get().get().getDocuments().forEach(noteDoc -> {
                try {
                    String firebaseNoteId = noteDoc.getId();
                    
                    // Find the corresponding note in our database
                    noteRepository.findAll().forEach(note -> {
                        try {
                            // Check if this note came from Firebase (has matching data)
                            String noteContent = noteDoc.getString("note");
                            if (noteContent == null) noteContent = noteDoc.getString("content");
                            
                            if (noteContent != null && noteContent.equals(note.getContent())) {
                                // Load comments subcollection
                                db.collection("notes").document(firebaseNoteId)
                                    .collection("comments").get().get().getDocuments().forEach(commentDoc -> {
                                    try {
                                        com.visiboard.backend.model.Comment comment = new com.visiboard.backend.model.Comment();
                                        
                                        // Map "text" field to content
                                        String text = commentDoc.getString("text");
                                        if (text == null) text = commentDoc.getString("content");
                                        comment.setContent(text);
                                        
                                        // Link to note
                                        comment.setNote(note);
                                        
                                        // Map user
                                        String userId = commentDoc.getString("userId");
                                        if (userId != null) {
                                            User user = userRepository.findByFirebaseUid(userId);
                                            if (user == null) {
                                                // Create temporary user from userName
                                                user = new User();
                                                user.setFirebaseUid(userId);
                                                user.setName(commentDoc.getString("userName"));
                                                user.setEmail("temp_" + userId + "@user.com");
                                                userRepository.save(user);
                                            }
                                            comment.setUser(user);
                                        }
                                        
                                        // Handle timestamp
                                        Object commentTimestampObj = commentDoc.get("timestamp");
                                        if (commentTimestampObj instanceof com.google.cloud.Timestamp) {
                                            comment.setCreatedAt(java.time.LocalDateTime.ofInstant(
                                                ((com.google.cloud.Timestamp) commentTimestampObj).toDate().toInstant(), 
                                                java.time.ZoneId.systemDefault()
                                            ));
                                        } else if (commentTimestampObj instanceof Long) {
                                            comment.setCreatedAt(java.time.LocalDateTime.ofInstant(
                                                java.time.Instant.ofEpochMilli((Long) commentTimestampObj), 
                                                java.time.ZoneId.systemDefault()
                                            ));
                                        } else {
                                            comment.setCreatedAt(java.time.LocalDateTime.now());
                                        }
                                        
                                        if (comment.getUser() != null) {
                                            commentRepository.save(comment);
                                            System.out.println("Saved comment for note: " + note.getId());
                                        }
                                    } catch (Exception e) {
                                        System.err.println("Error syncing comment " + commentDoc.getId() + ": " + e.getMessage());
                                    }
                                });
                            }
                        } catch (Exception e) {
                            System.err.println("Error processing note comments: " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    System.err.println("Error loading comments for note " + noteDoc.getId());
                }
            });
            System.out.println("Comments synced from Firebase");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
