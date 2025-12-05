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
        try {
            Firestore db = FirestoreClient.getFirestore();
            if (db == null) {
                System.err.println("[Firebase] Firebase not initialized! Cannot sync note.");
                return;
            }
            
            Map<String, Object> noteData = new HashMap<>();
            // Only the fields that Android uses
            noteData.put("note", note.getContent());
            noteData.put("lat", note.getLat());
            noteData.put("lon", note.getLng());
            noteData.put("location", new com.google.cloud.firestore.GeoPoint(note.getLat(), note.getLng()));
            noteData.put("userId", note.getUser().getFirebaseUid());
            noteData.put("userName", note.getUser().getName());
            noteData.put("likeCount", note.getLikesCount());
            noteData.put("likedBy", note.getLikedByUsers() != null ? note.getLikedByUsers() : new java.util.ArrayList<>());
            noteData.put("commentsCount", note.getCommentsCount());
            noteData.put("timestamp", com.google.cloud.Timestamp.now());

            String docId = note.getId().toString();
            System.out.println("[Firebase] Syncing note with fields: note, lat, lon, location, userId, userName, likeCount, likedBy, commentsCount, timestamp");
            db.collection("notes").document(docId).set(noteData).get();
            System.out.println("[Firebase] ✓ Successfully synced note to Firebase");
        } catch (Exception e) {
            System.err.println("[Firebase] ✗ Failed to sync note to Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void syncCommentToFirebase(com.visiboard.backend.model.Comment comment, Note note) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            if (db == null) {
                System.err.println("[Firebase] Firebase not initialized! Cannot sync comment.");
                return;
            }
            
            String noteDocId = note.getId().toString();
            String commentId = comment.getId().toString();
            
            Map<String, Object> commentData = new HashMap<>();
            // Only the exact fields Android uses: id, text, timestamp, userId, userName
            commentData.put("id", commentId);
            commentData.put("text", comment.getContent());
            commentData.put("userId", comment.getUser().getFirebaseUid());
            commentData.put("userName", comment.getUser().getName());
            commentData.put("timestamp", com.google.cloud.Timestamp.now());
            
            System.out.println("[Firebase] Syncing comment with fields: id, text, timestamp, userId, userName");
            db.collection("notes").document(noteDocId)
                .collection("comments").document(commentId).set(commentData).get();
            System.out.println("[Firebase] ✓ Successfully synced comment to Firebase");
        } catch (Exception e) {
            System.err.println("[Firebase] ✗ Failed to sync comment to Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteNoteFromFirebase(String noteId) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            if (db == null) {
                System.err.println("[Firebase] Firebase not initialized! Cannot delete note.");
                return;
            }
            
            System.out.println("[Firebase] Deleting note from Firebase: " + noteId);
            
            // Delete the note document and all its subcollections
            db.collection("notes").document(noteId).delete().get();
            System.out.println("[Firebase] ✓ Deleted note from Firebase: " + noteId);
            
        } catch (Exception e) {
            System.err.println("[Firebase] ✗ Failed to delete note from Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void syncFromFirebase() {
        System.out.println("Starting Firebase sync (preserving deleted notes tracking)...");
        
        // Check Firebase initialization
        Firestore db;
        try {
            db = FirestoreClient.getFirestore();
            if (db == null) {
                System.err.println("[Firebase] ✗ Firebase not initialized! Cannot sync.");
                System.err.println("[Firebase] Please ensure serviceAccountKey.json is in src/main/resources/");
                return;
            }
            System.out.println("[Firebase] ✓ Firebase connection established");
        } catch (Exception e) {
            System.err.println("[Firebase] ✗ Firebase error: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        try {
            // Clear all local data - Firebase is the source of truth
            commentRepository.deleteAll();
            noteLikeRepository.deleteAll();
            messageRepository.deleteAll();
            userFollowRepository.deleteAll();
            noteRepository.deleteAll();
            userRepository.deleteAll();
            System.out.println("Local database cleared.");
        } catch (Exception e) {
            System.err.println("Error clearing local database: " + e.getMessage());
            e.printStackTrace();
        }
        
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
                
                // If it's a raw base64 string, add the data URI prefix
                if (pic != null && !pic.startsWith("http") && !pic.startsWith("data:image/")) {
                    // Detect if it's base64 (starts with common image signatures)
                    if (pic.startsWith("/9j/") || pic.startsWith("iVBOR") || pic.startsWith("R0lGOD")) {
                        pic = "data:image/jpeg;base64," + pic;
                    }
                }
                
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
                            
                            // Fix profile pic if it's raw base64
                            String userPic = document.getString("userProfilePic");
                            if (userPic != null && !userPic.startsWith("http") && !userPic.startsWith("data:image/")) {
                                if (userPic.startsWith("/9j/") || userPic.startsWith("iVBOR") || userPic.startsWith("R0lGOD")) {
                                    userPic = "data:image/jpeg;base64," + userPic;
                                }
                            }
                            tempUser.setProfilePicUrl(userPic);
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

        // Sync Comments from subcollections (Optimized)
        try {
            System.out.println("Starting optimized comment sync from Firebase...");
            long startTime = System.currentTimeMillis();
            
            // Build lookup maps for O(1) access
            java.util.Map<String, Note> contentToNoteMap = new java.util.HashMap<>();
            java.util.List<Note> allNotes = noteRepository.findAll();
            for (Note n : allNotes) {
                if (n.getContent() != null && !n.getContent().isEmpty()) {
                    contentToNoteMap.put(n.getContent(), n);
                }
            }
            System.out.println("Built note lookup map with " + contentToNoteMap.size() + " entries");
            
            // Cache users for faster lookups
            java.util.Map<String, User> userCache = new java.util.HashMap<>();
            
            // Track synced comments in this session to avoid duplicates within the same sync
            java.util.Set<String> syncedComments = new java.util.HashSet<>();
            
            int syncedCount = 0;
            int skippedCount = 0;
            
            // Fetch all Firebase notes
            java.util.List<com.google.cloud.firestore.QueryDocumentSnapshot> firebaseNotes = 
                db.collection("notes").get().get().getDocuments();
            
            System.out.println("Processing " + firebaseNotes.size() + " Firebase notes for comments...");
            
            for (com.google.cloud.firestore.QueryDocumentSnapshot noteDoc : firebaseNotes) {
                try {
                    String firebaseNoteId = noteDoc.getId();
                    
                    // Get note content for matching
                    String noteContent = noteDoc.getString("note");
                    if (noteContent == null) noteContent = noteDoc.getString("content");
                    
                    Note dbNote = contentToNoteMap.get(noteContent);
                    if (dbNote == null) {
                        skippedCount++;
                        continue; // Note not in database
                    }
                    
                    // Fetch comments subcollection
                    java.util.List<com.google.cloud.firestore.QueryDocumentSnapshot> commentDocs = 
                        db.collection("notes").document(firebaseNoteId)
                            .collection("comments").get().get().getDocuments();
                    
                    if (commentDocs.isEmpty()) continue;
                    
                    for (com.google.cloud.firestore.QueryDocumentSnapshot commentDoc : commentDocs) {
                        try {
                            String text = commentDoc.getString("text");
                            if (text == null) text = commentDoc.getString("content");
                            if (text == null || text.isEmpty()) continue;
                            
                            // Check if already synced in this session (same note + same text)
                            String commentKey = dbNote.getId() + "|" + text;
                            if (syncedComments.contains(commentKey)) {
                                continue; // Skip duplicate within this sync
                            }
                            
                            com.visiboard.backend.model.Comment comment = new com.visiboard.backend.model.Comment();
                            comment.setContent(text);
                            comment.setNote(dbNote);
                            
                            // Get or create user
                            String userId = commentDoc.getString("userId");
                            if (userId != null) {
                                User user = userCache.get(userId);
                                if (user == null) {
                                    user = userRepository.findByFirebaseUid(userId);
                                    if (user == null) {
                                        user = new User();
                                        user.setFirebaseUid(userId);
                                        user.setName(commentDoc.getString("userName"));
                                        if (user.getName() == null) user.setName("Unknown");
                                        user.setEmail("temp_" + userId + "@user.com");
                                        user = userRepository.save(user);
                                    }
                                    userCache.put(userId, user);
                                }
                                comment.setUser(user);
                            }
                            
                            // Parse timestamp
                            Object timestampObj = commentDoc.get("timestamp");
                            if (timestampObj instanceof com.google.cloud.Timestamp) {
                                comment.setCreatedAt(java.time.LocalDateTime.ofInstant(
                                    ((com.google.cloud.Timestamp) timestampObj).toDate().toInstant(), 
                                    java.time.ZoneId.systemDefault()
                                ));
                            } else if (timestampObj instanceof Long) {
                                comment.setCreatedAt(java.time.LocalDateTime.ofInstant(
                                    java.time.Instant.ofEpochMilli((Long) timestampObj), 
                                    java.time.ZoneId.systemDefault()
                                ));
                            }
                            
                            if (comment.getUser() != null) {
                                commentRepository.save(comment);
                                syncedComments.add(commentKey);
                                syncedCount++;
                            }
                        } catch (Exception e) {
                            System.err.println("Error syncing comment: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error processing note comments: " + e.getMessage());
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("Comment sync complete: " + syncedCount + " synced, " + 
                             skippedCount + " notes skipped in " + duration + "ms");
        } catch (Exception e) {
            System.err.println("Fatal error syncing comments: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
