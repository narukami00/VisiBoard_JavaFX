package com.visiboard.pc.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.visiboard.pc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.Date;
import java.util.Arrays;

public class SyncService {

    private static volatile boolean initialSyncDone = false;
    private static Runnable syncCompleteCallback;

    public static boolean isInitialSyncDone() {
        return initialSyncDone;
    }

    public static void setSyncCompleteCallback(Runnable callback) {
        syncCompleteCallback = callback;
        // If already done, trigger immediately (logic handled by caller usually, but could be nice here too)
        // But for safety, caller checks isInitialSyncDone first.
    }

    public static void performInitialSync() {
        System.out.println("Starting Initial Sync...");
        syncUsers();
        syncNotes();
        syncNotes();
        syncReports();
        System.out.println("Initial Sync Completed.");
        
        initialSyncDone = true;
        if (syncCompleteCallback != null) {
            try {
                syncCompleteCallback.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void syncUsers() {
        Firestore db = FirebaseService.getFirestore();
        try {
            ApiFuture<QuerySnapshot> future = db.collection("users").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            System.out.println("Fetched " + documents.size() + " users from Firebase.");

            try (Connection conn = DatabaseService.getConnection()) {
                String sql = "INSERT INTO users (user_id, username, email, display_name, photo_url, created_at, total_likes_received, followers_count, following_count, is_banned, ban_expiry, is_restricted, restriction_expiry, synced_at) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                             "ON CONFLICT (user_id) DO UPDATE SET " +
                             "username = EXCLUDED.username, " +
                             "email = EXCLUDED.email, " +
                             "display_name = EXCLUDED.display_name, " +
                             "photo_url = EXCLUDED.photo_url, " +
                             "total_likes_received = EXCLUDED.total_likes_received, " +
                             "followers_count = EXCLUDED.followers_count, " +
                             "following_count = EXCLUDED.following_count, " +
                             "is_banned = EXCLUDED.is_banned, " +
                             "ban_expiry = EXCLUDED.ban_expiry, " +
                             "is_restricted = EXCLUDED.is_restricted, " +
                             "restriction_expiry = EXCLUDED.restriction_expiry, " +
                             "synced_at = CURRENT_TIMESTAMP";

                PreparedStatement pstmt = conn.prepareStatement(sql);

                for (QueryDocumentSnapshot doc : documents) {
                    // Robust field fetching (camelCase vs snake_case)
                    String userId = doc.getId();
                    String username = getString(doc, "username", "userName", "name"); // Fallback for name
                    String email = getString(doc, "email");
                    String displayName = getString(doc, "displayName", "display_name", "name"); // Fallback chain
                    String photoUrl = getString(doc, "photoUrl", "photo_url", "profilePicUrl", "profile_pic_url", "profilePic");
                    
                    // Stats
                    Long likes = getLong(doc, "totalLikesReceived", "totalLikes");
                    Long followers = getLong(doc, "followersCount", "followers");
                    Long following = doc.getLong("followingCount");
                    
                    // Moderation
                    Boolean banned = doc.getBoolean("banned");
                    if (banned == null) banned = doc.getBoolean("isBanned");
                    if (banned == null) banned = false;
                    
                    Long banExpiry = getLong(doc, "banExpiryDate", "banExpiry");
                    
                    Boolean restricted = doc.getBoolean("restricted");
                    if (restricted == null) restricted = doc.getBoolean("isRestricted");
                    if (restricted == null) restricted = false;
                    
                    Long restrictExpiry = getLong(doc, "restrictionExpiryDate", "restrictionExpiry");
                    
                    int totalLikes = likes != null ? likes.intValue() : 0;
                    int followersCount = followers != null ? followers.intValue() : 0;
                    int followingCount = following != null ? following.intValue() : 0;

                    pstmt.setString(1, userId);
                    pstmt.setString(2, username);
                    pstmt.setString(3, email);
                    pstmt.setString(4, displayName);
                    pstmt.setString(5, photoUrl);
                    pstmt.setTimestamp(6, new Timestamp(System.currentTimeMillis())); // Placeholder for CreatedAt
                    
                    pstmt.setInt(7, totalLikes);
                    pstmt.setInt(8, followersCount);
                    pstmt.setInt(9, followingCount);
                    
                    pstmt.setBoolean(10, banned);
                    pstmt.setLong(11, banExpiry != null ? banExpiry : 0);
                    pstmt.setBoolean(12, restricted);
                    pstmt.setLong(13, restrictExpiry != null ? restrictExpiry : 0);

                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                System.out.println("Users synced to PostgreSQL.");

            } catch (SQLException e) {
                e.printStackTrace();
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static void syncNotes() {
        Firestore db = FirebaseService.getFirestore();
        try {
            ApiFuture<QuerySnapshot> future = db.collection("notes").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            System.out.println("Fetched " + documents.size() + " notes from Firebase.");

            try (Connection conn = DatabaseService.getConnection()) {
                String sql = "INSERT INTO notes (note_id, user_id, content, image_url, latitude, longitude, likes_count, is_hidden, created_at, liked_by_users, synced_at) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                             "ON CONFLICT (note_id) DO UPDATE SET " +
                             "content = EXCLUDED.content, " +
                             "image_url = EXCLUDED.image_url, " +
                             "likes_count = EXCLUDED.likes_count, " +
                             "is_hidden = EXCLUDED.is_hidden, " +
                             "liked_by_users = EXCLUDED.liked_by_users, " +
                             "synced_at = CURRENT_TIMESTAMP";

                PreparedStatement pstmt = conn.prepareStatement(sql);

                for (QueryDocumentSnapshot doc : documents) {
                    pstmt.setString(1, doc.getId());
                    pstmt.setString(2, getString(doc, "userId", "user_id"));
                    pstmt.setString(3, getString(doc, "text", "note", "content"));
                    pstmt.setString(4, getString(doc, "imageUrl", "image_url", "imageBase64"));
                    Double lat = getDouble(doc, "latitude", "lat");
                    Double lon = getDouble(doc, "longitude", "lng", "lon");
                    pstmt.setDouble(5, lat != null ? lat : 0.0);
                    pstmt.setDouble(6, lon != null ? lon : 0.0);
                    Long likes = getLong(doc, "likesCount", "likeCount", "likes");
                    pstmt.setInt(7, likes != null ? likes.intValue() : 0);
                    Boolean hidden = doc.getBoolean("isHidden");
                    pstmt.setBoolean(8, hidden != null && hidden);

                    // Handle Timestamp
                    Object createdObj = doc.get("createdAt");
                    if (createdObj == null) createdObj = doc.get("timestamp"); // Fallback
                    
                    if (createdObj instanceof com.google.cloud.Timestamp) {
                        pstmt.setTimestamp(9, new Timestamp(((com.google.cloud.Timestamp) createdObj).toDate().getTime()));
                    } else if (createdObj instanceof Long) {
                        pstmt.setTimestamp(9, new Timestamp((Long) createdObj));
                    } else {
                        pstmt.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
                    }

                    // Handle likedByUsers
                    List<String> likedBy = (List<String>) doc.get("likedByUsers");
                    if (likedBy != null) {
                        java.sql.Array array = conn.createArrayOf("text", likedBy.toArray());
                        pstmt.setArray(10, array);
                    } else {
                        pstmt.setArray(10, null);
                    }

                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                System.out.println("Notes synced to PostgreSQL.");

            } catch (SQLException e) {
                e.printStackTrace();
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static void syncReports() {
        Firestore db = FirebaseService.getFirestore();
        try {
            ApiFuture<QuerySnapshot> future = db.collection("reports").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            System.out.println("Fetched " + documents.size() + " reports from Firebase.");

            try (Connection conn = DatabaseService.getConnection()) {
                String sql = "INSERT INTO reports (report_id, reporter_id, reported_user_id, reported_note_id, reason, target_details, type, timestamp, status, synced_at) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                             "ON CONFLICT (report_id) DO UPDATE SET " +
                             "status = EXCLUDED.status, " +
                             "synced_at = CURRENT_TIMESTAMP";

                PreparedStatement pstmt = conn.prepareStatement(sql);

                for (QueryDocumentSnapshot doc : documents) {
                    pstmt.setString(1, doc.getId());
                    pstmt.setString(2, getString(doc, "reporterId", "reporter_id"));
                    
                    String type = getString(doc, "type"); // NOTE or USER
                    String targetId = getString(doc, "targetId", "target_id");
                    
                    // Map targetId to specific columns based on type
                    String reportedUser = getString(doc, "reportedUserId", "reported_user_id"); // Fallback legacy
                    String reportedNote = getString(doc, "reportedNoteId", "reported_note_id"); // Fallback legacy
                    
                    if ("NOTE".equalsIgnoreCase(type)) {
                        if (targetId != null) reportedNote = targetId;
                    } else {
                         // Default to user report if not Note
                        if (targetId != null) reportedUser = targetId;
                    }

                    pstmt.setString(3, reportedUser);
                    pstmt.setString(4, reportedNote);
                    
                    // Reason: try reason, then description
                    pstmt.setString(5, getString(doc, "reason", "description"));
                    pstmt.setString(6, getString(doc, "targetDetails", "target_details"));
                    pstmt.setString(7, type);
                    
                    Long ts = doc.getLong("timestamp");
                    pstmt.setLong(8, ts != null ? ts : System.currentTimeMillis());
                    
                    String status = doc.getString("status");
                    pstmt.setString(9, status != null ? status : "PENDING");

                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                System.out.println("Reports synced to PostgreSQL.");

            } catch (SQLException e) {
                e.printStackTrace();
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static String getString(QueryDocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            if (doc.contains(key)) {
                String val = doc.getString(key);
                if (val != null) return val;
            }
        }
        return null;
    }

    private static Long getLong(QueryDocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            if (doc.contains(key)) {
                Long val = doc.getLong(key);
                if (val != null) return val;
            }
        }
        return null;
    }

    private static Double getDouble(QueryDocumentSnapshot doc, String... keys) {
        for (String key : keys) {
            if (doc.contains(key)) {
                Double val = doc.getDouble(key);
                if (val != null) return val;
            }
        }
        return null;
    }
}
