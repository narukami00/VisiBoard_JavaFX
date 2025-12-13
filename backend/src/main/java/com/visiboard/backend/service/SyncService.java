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
            noteData.put("userProfilePic", note.getUser().getProfilePicUrl());
            noteData.put("likeCount", note.getLikesCount());
            noteData.put("commentsCount", note.getCommentsCount());
            
            // Image Data
            if (note.getImageBase64() != null) {
                noteData.put("imageBase64", note.getImageBase64());
                noteData.put("imageWidth", note.getImageWidth());
                noteData.put("imageHeight", note.getImageHeight());
            }
            
            // CRITICAL FIX: Do NOT send 'likedBy' list here. It causes overwrites.
            // Likes are handled atomically via updateNoteLikeInFirebase.
            // noteData.put("likedBy", ...); 

            String docId = note.getFirebaseId();
            boolean isNewNote = (docId == null || docId.isEmpty());
            
            if (isNewNote) {
                // New note from PC, generate ID
                docId = db.collection("notes").document().getId();
                note.setFirebaseId(docId);
                // Save the firebaseId back to local DB
                noteRepository.save(note);
                
                // Only set timestamp for NEW notes
                if (note.getCreatedAt() != null) {
                    noteData.put("timestamp", note.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
                } else {
                    noteData.put("timestamp", System.currentTimeMillis());
                }
            } else {
                // For existing notes, DO NOT touch the timestamp
                // This prevents the "timestamp update on comment" bug
            }
            
            System.out.println("[Firebase] Syncing note " + docId + " (isNew=" + isNewNote + ")");
            
            if (isNewNote) {
                db.collection("notes").document(docId).set(noteData).get();
            } else {
                // For updates, use update() to avoid overwriting missing fields (like timestamp if we omitted it)
                // But set() with Merge is better if we want to ensure fields exist. 
                // However, standard set() overwrites everything not in the map.
                // Let's use set(noteData, SetOptions.merge()) equivalent logic or just update specific fields.
                // Since we removed timestamp/likedBy from map, set() would delete them if we don't use merge.
                db.collection("notes").document(docId).set(noteData, com.google.cloud.firestore.SetOptions.merge()).get();
            }
            System.out.println("[Firebase] ✓ Successfully synced note to Firebase: " + docId);
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
            
            String noteDocId = note.getFirebaseId();
            if (noteDocId == null) {
                System.out.println("[Firebase] Parent note has no Firebase ID, syncing note first...");
                syncNoteToFirebase(note);
                noteDocId = note.getFirebaseId();
                if (noteDocId == null) {
                    System.err.println("[Firebase] Failed to generate Firebase ID for parent note. Cannot sync comment.");
                    return;
                }
            }
            
            String commentId = comment.getFirebaseId();
            if (commentId == null || commentId.isEmpty()) {
                commentId = java.util.UUID.randomUUID().toString(); // Or generate from Firestore
                comment.setFirebaseId(commentId);
                commentRepository.save(comment);
            }
            
            String userId = comment.getUser().getFirebaseUid();
            if (userId == null) {
                System.err.println("[Firebase] Comment user has no Firebase UID. Cannot sync.");
                return;
            }
            
            Map<String, Object> commentData = new HashMap<>();
            // Only the exact fields Android uses: id, text, timestamp, userId, userName
            commentData.put("id", commentId);
            commentData.put("text", comment.getContent());
            commentData.put("userId", userId);
            commentData.put("userName", comment.getUser().getName());
            // Android uses Long for timestamp
            commentData.put("timestamp", System.currentTimeMillis());
            
            String path = "notes/" + noteDocId + "/comments/" + commentId;
            System.out.println("[Firebase] Syncing comment to path: " + path);
            
            db.collection("notes").document(noteDocId)
                .collection("comments").document(commentId).set(commentData).get();
            System.out.println("[Firebase] ✓ Successfully synced comment to Firebase: " + commentId);
        } catch (Exception e) {
            System.err.println("[Firebase] ✗ Failed to sync comment to Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateNoteLikeInFirebase(String noteId, String userId, boolean isLiked) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            if (db == null) return;
            
            // Lookup note to get firebaseId if it's a UUID
            String firebaseId = noteId;
            try {
                java.util.UUID uuid = java.util.UUID.fromString(noteId);
                Note note = noteRepository.findById(uuid).orElse(null);
                if (note != null && note.getFirebaseId() != null) {
                    firebaseId = note.getFirebaseId();
                }
            } catch (IllegalArgumentException e) {
                // Not a UUID, assume it's already a firebaseId
            }
            
            com.google.cloud.firestore.DocumentReference docRef = db.collection("notes").document(firebaseId);
            
            if (isLiked) {
                // Add user to likedBy array and increment count
                docRef.update(
                    "likedBy", com.google.cloud.firestore.FieldValue.arrayUnion(userId),
                    "likeCount", com.google.cloud.firestore.FieldValue.increment(1)
                ).get();
                System.out.println("[Firebase] Added like for user " + userId + " to note " + firebaseId);
            } else {
                // Remove user from likedBy array and decrement count
                docRef.update(
                    "likedBy", com.google.cloud.firestore.FieldValue.arrayRemove(userId),
                    "likeCount", com.google.cloud.firestore.FieldValue.increment(-1)
                ).get();
                System.out.println("[Firebase] Removed like for user " + userId + " from note " + firebaseId);
            }
        } catch (Exception e) {
            System.err.println("[Firebase] Failed to update like status: " + e.getMessage());
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
            // We need to find the note by ID first to get its firebaseId if passed as UUID string
            // But here we expect noteId to be the firebaseId if called correctly. 
            // However, the controller passes UUID.toString(). We need to fix that or lookup.
            
            // Lookup note to get firebaseId if it's a UUID
            String firebaseId = noteId;
            try {
                java.util.UUID uuid = java.util.UUID.fromString(noteId);
                // It's a UUID, look it up
                Note note = noteRepository.findById(uuid).orElse(null);
                if (note != null && note.getFirebaseId() != null) {
                    firebaseId = note.getFirebaseId();
                }
            } catch (IllegalArgumentException e) {
                // Not a UUID, assume it's already a firebaseId
            }
            
            System.out.println("[Firebase] Deleting note from Firebase: " + firebaseId);

            // Recursively delete subcollections (e.g., comments)
            Iterable<com.google.cloud.firestore.CollectionReference> collections = 
                db.collection("notes").document(firebaseId).listCollections();
            
            for (com.google.cloud.firestore.CollectionReference collection : collections) {
                deleteCollection(collection, 50);
            }
            
            // Delete the note document
            db.collection("notes").document(firebaseId).delete().get();
            System.out.println("[Firebase] ✓ Deleted note from Firebase: " + firebaseId);
            
        } catch (Exception e) {
            System.err.println("[Firebase] ✗ Failed to delete note from Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Helper method to delete a collection by deleting all documents in batches.
     */
    private void deleteCollection(com.google.cloud.firestore.CollectionReference collection, int batchSize) {
        try {
            // Retrieve a small batch of documents to avoid out-of-memory errors
            com.google.api.core.ApiFuture<com.google.cloud.firestore.QuerySnapshot> future = collection.limit(batchSize).get();
            int deleted = 0;
            // Future.get() blocks on response
            java.util.List<com.google.cloud.firestore.QueryDocumentSnapshot> documents = future.get().getDocuments();
            for (com.google.cloud.firestore.QueryDocumentSnapshot document : documents) {
                // Recursively delete subcollections of this document if any (though for comments likely not needed, but good practice)
                Iterable<com.google.cloud.firestore.CollectionReference> subCollections = document.getReference().listCollections();
                for (com.google.cloud.firestore.CollectionReference subCollection : subCollections) {
                    deleteCollection(subCollection, batchSize);
                }
                
                document.getReference().delete();
                ++deleted;
            }
            if (deleted >= batchSize) {
                // retrieve and delete another batch
                deleteCollection(collection, batchSize);
            }
        } catch (Exception e) {
            System.err.println("Error deleting collection " + collection.getId() + ": " + e.getMessage());
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
            // Delete notifications first because they reference users and notes
            notificationRepository.deleteAll();
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
                
                user.setProfilePicUrl(pic);
                
                // Stats Mapping
                Long likes = document.getLong("totalLikes");
                user.setTotalLikesReceived(likes != null ? likes.intValue() : 0);
                
                Long followers = document.getLong("followersCount");
                user.setFollowersCount(followers != null ? followers.intValue() : 0);
                
                Long following = document.getLong("followingCount");
                user.setFollowingCount(following != null ? following.intValue() : 0);
                
                // Timestamp Mapping
                Long created = document.getLong("createdAt");
                if (created != null) {
                    user.setCreatedAt(java.time.LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(created), 
                        java.time.ZoneId.systemDefault()
                    ));
                } else {
                    // Try parsing from string if legacy
                    try {
                        String createdStr = document.getString("createdAt");
                        if (createdStr != null) {
                           user.setCreatedAt(java.time.LocalDateTime.parse(createdStr));
                        }
                    } catch(Exception e) {
                        // Ignore
                    }
                }
                
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
                    note.setFirebaseId(document.getId());
                    
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

                    // Image Mapping
                    String imgBase64 = document.getString("imageBase64");
                    if (imgBase64 != null) {
                        note.setImageBase64(imgBase64);
                        Long w = document.getLong("imageWidth");
                        Long h = document.getLong("imageHeight");
                        note.setImageWidth(w != null ? w.intValue() : null);
                        note.setImageHeight(h != null ? h.intValue() : null);
                    }

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
                    
                    // LikedBy Mapping (CRITICAL for persistence)
                    java.util.List<String> likedBy = (java.util.List<String>) document.get("likedBy");
                    if (likedBy != null) {
                        for (String likerUid : likedBy) {
                            try {
                                User liker = userRepository.findByFirebaseUid(likerUid);
                                if (liker == null) {
                                    // Create temp user for liker if missing
                                    liker = new User();
                                    liker.setFirebaseUid(likerUid);
                                    liker.setEmail("liker_" + likerUid + "@user.com");
                                    liker.setName("Unknown Liker");
                                    liker.setProfilePicUrl("https://ui-avatars.com/api/?name=UL&background=random");
                                    userRepository.save(liker);
                                }
                                
                                // Create local NoteLike
                                com.visiboard.backend.model.NoteLike noteLike = new com.visiboard.backend.model.NoteLike(liker, note);
                                noteLikeRepository.save(noteLike);
                            } catch (Exception e) {
                                System.err.println("Error syncing like for user " + likerUid + ": " + e.getMessage());
                            }
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
                            comment.setFirebaseId(commentDoc.getId());
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
        
        // Sync Notifications
        syncNotificationsFromFirebase();
        
        // Sync Messages
        syncMessagesFromFirebase();
    }
    public void syncNotificationsFromFirebase() {
        System.out.println("Starting Notification sync from Firebase...");
        Firestore db = FirestoreClient.getFirestore();
        if (db == null) return;
        
        try {
            // First, delete all existing notifications to avoid stale data (optional, but safer for full valid sync)
            try {
                notificationRepository.deleteAll();
            } catch (Exception e) {
                System.err.println("Error deleting existing notifications: " + e.getMessage());
            }
            
            // Fetch all notifications from Firebase
            java.util.List<com.google.cloud.firestore.QueryDocumentSnapshot> documents = 
                db.collection("notifications").get().get().getDocuments();
            
            System.out.println("Found " + documents.size() + " notifications in Firebase");
            
            int syncedCount = 0;
            
            for (com.google.cloud.firestore.QueryDocumentSnapshot doc : documents) {
                try {
                    String type = doc.getString("type");
                    String toUserId = doc.getString("toUserId");
                    String fromUserId = doc.getString("fromUserId");
                    String noteId = doc.getString("noteId");
                    Long timestamp = doc.getLong("timestamp");
                    Boolean read = doc.getBoolean("read");
                    
                    if (toUserId == null || fromUserId == null) continue;
                    
                    // Find Recipient
                    User recipient = userRepository.findByFirebaseUid(toUserId);
                    if (recipient == null) {
                        // Create temp recipient? Or skip? Better to skip if user doesn't exist locally yet
                        // But maybe we should fetch them. For now, assume users are synced.
                        continue; 
                    }
                    
                    // Find Sender
                    User sender = userRepository.findByFirebaseUid(fromUserId);
                    if (sender == null) {
                        sender = new User();
                        sender.setFirebaseUid(fromUserId);
                        sender.setName(doc.getString("fromUserName") != null ? doc.getString("fromUserName") : "Unknown");
                        sender.setEmail("temp_sender_" + fromUserId + "@user.com");
                        String pic = doc.getString("fromUserProfilePic");
                        // Fix base64 prefixes if needed
                        if (pic != null && !pic.startsWith("http") && !pic.startsWith("data:image/")) {
                            if (pic.startsWith("/9j/") || pic.startsWith("iVBOR") || pic.startsWith("R0lGOD")) {
                                pic = "data:image/jpeg;base64," + pic;
                            }
                        }
                        sender.setProfilePicUrl(pic);
                        sender = userRepository.save(sender);
                    }
                    
                    // Find Note
                    Note note = null;
                    if (noteId != null) {
                        try {
                            // First try as UUID
                            java.util.UUID uuid = java.util.UUID.fromString(noteId);
                            note = noteRepository.findById(uuid).orElse(null);
                        } catch (IllegalArgumentException e) {
                            // If not UUID, it might be Firebase ID, but we strictly store UUIDs as IDs in Postgres
                            // and FirebaseID as a field. So we need to find by firebaseId
                            note = noteRepository.findByFirebaseId(noteId);
                        }
                    }
                    
                    com.visiboard.backend.model.Notification notification = new com.visiboard.backend.model.Notification();
                    notification.setRecipient(recipient);
                    notification.setSender(sender);
                    notification.setNote(note);
                    notification.setType(type != null ? type.toUpperCase() : "UNKNOWN");
                    notification.setMessage(doc.getString("messageText")); // Assuming messageText field exists for messages
                    
                    // If message is null, construct based on type (PC client does this, but backend should store raw if available)
                    // The Android model has 'messageText'.
                    
                    notification.setRead(read != null ? read : false);
                    
                    if (timestamp != null) {
                        notification.setCreatedAt(java.time.LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(timestamp), 
                            java.time.ZoneId.systemDefault()
                        ));
                    } else {
                        notification.setCreatedAt(java.time.LocalDateTime.now());
                    }
                    
                    notificationRepository.save(notification);
                    syncedCount++;
                    
                } catch (Exception e) {
                    System.err.println("Error syncing notification " + doc.getId() + ": " + e.getMessage());
                }
            }
            
            System.out.println("Synced " + syncedCount + " notifications");
            
        } catch (Exception e) {
            System.err.println("Fatal error syncing notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void syncMessagesFromFirebase() {
        System.out.println("Starting Message sync from Firebase...");
        Firestore db = FirestoreClient.getFirestore();
        if (db == null) return;
        
        try {
            // Fetch all messages from Firebase
            java.util.List<com.google.cloud.firestore.QueryDocumentSnapshot> documents = 
                db.collection("messages").get().get().getDocuments();
            
            System.out.println("Found " + documents.size() + " messages in Firebase");
            
            int syncedCount = 0;
            
            for (com.google.cloud.firestore.QueryDocumentSnapshot doc : documents) {
                try {
                    String fromUserId = doc.getString("fromUserId");
                    String toUserId = doc.getString("toUserId");
                    String messageText = doc.getString("messageText");
                    Long timestamp = doc.getLong("timestamp");
                    Boolean read = doc.getBoolean("read");
                    Boolean anonymous = doc.getBoolean("anonymous");
                    
                    if (fromUserId == null || toUserId == null) continue;
                    
                    // Find Sender
                    User sender = userRepository.findByFirebaseUid(fromUserId);
                    if (sender == null) {
                        sender = new User();
                        sender.setFirebaseUid(fromUserId);
                        sender.setName(doc.getString("fromUserName") != null ? doc.getString("fromUserName") : "Unknown");
                        sender.setEmail("temp_sender_" + fromUserId + "@user.com");
                        String pic = doc.getString("fromUserProfilePic");
                        if (pic != null && !pic.startsWith("http") && !pic.startsWith("data:image/")) {
                             if (pic.startsWith("/9j/") || pic.startsWith("iVBOR") || pic.startsWith("R0lGOD")) {
                                 pic = "data:image/jpeg;base64," + pic;
                             }
                        }
                        sender.setProfilePicUrl(pic);
                        sender = userRepository.save(sender);
                    }
                    
                    // Find Recipient
                    User recipient = userRepository.findByFirebaseUid(toUserId);
                    if (recipient == null) {
                        // If recipient missing, try to create temp? Or skip?
                        // Let's create temp to ensure message is saved
                        recipient = new User();
                        recipient.setFirebaseUid(toUserId);
                        recipient.setName("Unknown Recipient");
                        recipient.setEmail("temp_recipient_" + toUserId + "@user.com");
                        recipient = userRepository.save(recipient);
                    }
                    
                    com.visiboard.backend.model.Message message = new com.visiboard.backend.model.Message();
                    message.setFirebaseId(doc.getId());
                    message.setFromUser(sender);
                    message.setToUser(recipient);
                    message.setText(messageText);
                    message.setRead(read != null ? read : false);
                    message.setAnonymous(anonymous != null ? anonymous : false);
                    
                    if (timestamp != null) {
                        message.setCreatedAt(java.time.LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(timestamp), 
                            java.time.ZoneId.systemDefault()
                        ));
                    } else {
                        message.setCreatedAt(java.time.LocalDateTime.now());
                    }
                    
                    messageRepository.save(message);
                    syncedCount++;
                    
                } catch (Exception e) {
                    System.err.println("Error syncing message " + doc.getId() + ": " + e.getMessage());
                }
            }
            
            System.out.println("Synced " + syncedCount + " messages");
            
        } catch (Exception e) {
            System.err.println("Fatal error syncing messages: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

