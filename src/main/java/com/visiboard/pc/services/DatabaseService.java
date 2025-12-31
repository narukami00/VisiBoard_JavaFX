package com.visiboard.pc.services;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseService {

    private static HikariDataSource dataSource;

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/visiboard";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "ClashRoyale!1";

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Read schema from resources or file logic (simplified for immediate fix)
            // Ideally we'd read schema.sql, but for now we'll execute the CREATE TABLEs directly
            // to ensure they exist match schema.sql exactly.
            
            // DROP tables to ensure clean schema (since we had schema mismatches)
            // SyncService will Repopulate data.
            stmt.execute("DROP TABLE IF EXISTS notifications CASCADE");
            stmt.execute("DROP TABLE IF EXISTS reports CASCADE");
            stmt.execute("DROP TABLE IF EXISTS notes CASCADE");
            stmt.execute("DROP TABLE IF EXISTS users CASCADE");
            
            // Users Table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                         "user_id VARCHAR(255) PRIMARY KEY, " +
                         "username VARCHAR(255), " +
                         "email VARCHAR(255), " +
                         "display_name VARCHAR(255), " +
                         "photo_url TEXT, " +
                         "is_banned BOOLEAN DEFAULT FALSE, " +
                         "ban_expiry BIGINT, " +
                         "is_restricted BOOLEAN DEFAULT FALSE, " +
                         "restriction_expiry BIGINT, " +
                         "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                         "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                         "synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                         "total_likes_received INT DEFAULT 0, " + // Missing in schema.sql but used in code
                         "followers_count INT DEFAULT 0, " +       // Missing in schema.sql but used in code
                         "following_count INT DEFAULT 0" +         // Missing in schema.sql but used in code
                         ")");

            // Notes Table
            stmt.execute("CREATE TABLE IF NOT EXISTS notes (" +
                         "note_id VARCHAR(255) PRIMARY KEY, " +
                         "user_id VARCHAR(255) REFERENCES users(user_id), " +
                         "content TEXT, " +
                         "image_url TEXT, " +
                         "latitude DOUBLE PRECISION, " +
                         "longitude DOUBLE PRECISION, " +
                         "likes_count INT DEFAULT 0, " +
                         "is_hidden BOOLEAN DEFAULT FALSE, " +
                         "created_at TIMESTAMP, " +
                         "liked_by_users TEXT[], " +
                         "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                         "synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                         ")");

            // Reports Table
            stmt.execute("CREATE TABLE IF NOT EXISTS reports (" +
                         "report_id VARCHAR(255) PRIMARY KEY, " +
                         "reporter_id VARCHAR(255), " +
                         "reported_user_id VARCHAR(255), " +
                         "reported_note_id VARCHAR(255), " +
                         "reason TEXT, " +
                         "target_details TEXT, " +
                         "type VARCHAR(50), " +
                         "timestamp BIGINT, " +
                         "status VARCHAR(50) DEFAULT 'PENDING', " +
                         "synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                         ")");
                         


            // Notifications Table
            stmt.execute("CREATE TABLE IF NOT EXISTS notifications (" +
                         "notification_id VARCHAR(255) PRIMARY KEY, " +
                         "user_id VARCHAR(255), " +
                         "message TEXT, " +
                         "type VARCHAR(50), " +
                         "is_read BOOLEAN DEFAULT FALSE, " +
                         "created_at BIGINT" +
                         ")");

            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
    }
    
    public static int getRecordCount(String tableName) {
        String query = "SELECT COUNT(*) FROM " + tableName;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(query)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
             // Ignore
        }
        return 0;
    }

    public static void updateUserStatus(String userId, String statusField, boolean value, long expiryTimestamp) {
        String boolColumn = "";
        String expiryColumn = "";
        
        if ("banned".equalsIgnoreCase(statusField)) {
            boolColumn = "is_banned";
            expiryColumn = "ban_expiry";
        } else if ("restricted".equalsIgnoreCase(statusField)) {
            boolColumn = "is_restricted";
            expiryColumn = "restriction_expiry";
        } else {
             System.err.println("Invalid status field: " + statusField);
             return;
        }

        String sql = "UPDATE users SET " + boolColumn + " = ?, " + expiryColumn + " = ? WHERE user_id = ?";
        try (Connection conn = getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, value);
            pstmt.setLong(2, expiryTimestamp);
            pstmt.setString(3, userId);
            pstmt.executeUpdate();
            System.out.println("Updated user " + userId + " " + statusField + "=" + value + ", expiry=" + expiryTimestamp);
            
            // Sync to Firebase? 
            // Ideally we need a 'syncToFirebase' method or 'FirebaseService.updateUser'.
            // For now, DatabaseService is the primary touchpoint. 
            // In a real app we'd trigger Firebase update here too. 
            // I'll add a TODO or call FirebaseService if it exists.
            com.visiboard.pc.services.FirebaseService.updateUserField(userId, statusField, value);
            if (value) {
                // If turning ON, also set expiry
                String firestoreExpiryField = "banned".equalsIgnoreCase(statusField) ? "banExpiryDate" : "restrictionExpiryDate";
                com.visiboard.pc.services.FirebaseService.updateUserField(userId, firestoreExpiryField, expiryTimestamp);
            } else {
                 // If turning OFF, clear expiry? Optional, but good practice.
                 String firestoreExpiryField = "banned".equalsIgnoreCase(statusField) ? "banExpiryDate" : "restrictionExpiryDate";
                com.visiboard.pc.services.FirebaseService.updateUserField(userId, firestoreExpiryField, 0L);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Overload for backward compatibility / non-expiry calls (e.g. initial fix) - mapped to 0 expiry
    public static void updateUserStatus(String userId, String statusField, boolean value) {
        updateUserStatus(userId, statusField, value, 0);
    }

    public static void notifyUser(String userId, String message) {
        notifyUser(userId, message, "admin");
    }

    public static void notifyUser(String userId, String message, String type) {
        String sql = "INSERT INTO notifications (notification_id, user_id, message, type, is_read, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String notifId = java.util.UUID.randomUUID().toString();
            long timestamp = System.currentTimeMillis();
            
            pstmt.setString(1, notifId);
            pstmt.setString(2, userId);
            pstmt.setString(3, message);
            pstmt.setString(4, type);
            pstmt.setBoolean(5, false);
            pstmt.setLong(6, timestamp);
            
            pstmt.executeUpdate();
            System.out.println("Notification sent to " + userId + ": " + message);
            
            // Sync to Firebase
            java.util.Map<String, Object> notifData = new java.util.HashMap<>();
            notifData.put("toUserId", userId);
            notifData.put("messageText", message);
            notifData.put("type", type);
            notifData.put("read", false);
            notifData.put("timestamp", timestamp);
            
            // Standard fields for Admin notifications
            notifData.put("fromUserId", "ADMIN_PANEL"); 
            notifData.put("fromUserName", "VisiBoard Admin");
            notifData.put("fromUserProfilePic", "https://ui-avatars.com/api/?name=VisiBoard+Admin&background=000&color=fff");
            notifData.put("noteId", null);
            notifData.put("noteText", null);
            
            com.visiboard.pc.services.FirebaseService.sendNotification(notifData);
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to notify user: " + e.getMessage());
        }
    }
    
    public static java.util.List<com.visiboard.pc.model.User> getAllUsers() {
        java.util.List<com.visiboard.pc.model.User> users = new java.util.ArrayList<>();
        String query = "SELECT * FROM users ORDER BY created_at DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                com.visiboard.pc.model.User user = new com.visiboard.pc.model.User();
                // Map DB columns (Local Postgres Schema) to Model
                
                // Use user_id as the main ID for the PC app
                String fid = rs.getString("user_id");
                // IF user_id is null/empty check if there is another ID column? No, user_id is PK.
                user.setId(fid);
                user.setFirebaseUid(fid);
                
                user.setName(rs.getString("display_name")); // Local DB column: display_name
                user.setEmail(rs.getString("email"));
                user.setProfilePicUrl(rs.getString("photo_url")); // Local DB column: photo_url
                
                // Moderation Status
                user.setBanned(rs.getBoolean("is_banned"));
                user.setBanExpiry(rs.getLong("ban_expiry"));
                user.setRestricted(rs.getBoolean("is_restricted"));
                user.setRestrictionExpiry(rs.getLong("restriction_expiry"));
                
                // Handle Timestamp
                java.sql.Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) {
                    user.setCreatedAt(ts.getTime());
                }
                
                try {
                    user.setTotalLikesReceived(rs.getInt("total_likes_received"));
                    user.setFollowersCount(rs.getInt("followers_count"));
                    user.setFollowingCount(rs.getInt("following_count"));
                } catch (SQLException ex) {
                    // Ignore missing stats columns
                }
                
                users.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to fetch all users: " + e.getMessage());
        }
        return users;
    }

    public static java.util.List<com.visiboard.pc.model.Report> getAllReports() {
        java.util.List<com.visiboard.pc.model.Report> reports = new java.util.ArrayList<>();
        String query = "SELECT r.*, " +
                "u1.display_name as reporter_name, " +
                "COALESCE(u2.display_name, u3.display_name) as reported_name, " +
                "COALESCE(r.reported_user_id, n.user_id) as effective_reported_user_id " +
                "FROM reports r " +
                "LEFT JOIN users u1 ON r.reporter_id = u1.user_id " +
                "LEFT JOIN users u2 ON r.reported_user_id = u2.user_id " +
                "LEFT JOIN notes n ON r.reported_note_id = n.note_id " +
                "LEFT JOIN users u3 ON n.user_id = u3.user_id " +
                "ORDER BY r.timestamp DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                com.visiboard.pc.model.Report report = new com.visiboard.pc.model.Report();
                report.setReportId(rs.getString("report_id"));
                report.setReporterId(rs.getString("reporter_id"));
                // Use the effective ID found directly or via note
                report.setReportedUserId(rs.getString("effective_reported_user_id"));
                report.setReportedNoteId(rs.getString("reported_note_id"));
                report.setReason(rs.getString("reason"));
                report.setTargetDetails(rs.getString("target_details"));
                report.setType(rs.getString("type"));
                // timestamp is BIGINT in reports table, so getLong is safe
                report.setTimestamp(rs.getLong("timestamp"));
                report.setStatus(rs.getString("status"));

                // Set resolved names
                report.setReporterName(rs.getString("reporter_name"));
                report.setReportedName(rs.getString("reported_name"));

                reports.add(report);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to fetch all reports: " + e.getMessage());
        }
        return reports;
    }
    public static java.util.List<com.visiboard.pc.model.Note> getNotesByUserId(String userId) {
        java.util.List<com.visiboard.pc.model.Note> notes = new java.util.ArrayList<>();
        String query = "SELECT * FROM notes WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, userId);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    com.visiboard.pc.model.Note note = new com.visiboard.pc.model.Note();
                    note.setNoteId(rs.getString("note_id"));
                    note.setUserId(rs.getString("user_id"));
                    note.setContent(rs.getString("content"));
                    note.setImageUrl(rs.getString("image_url"));
                    note.setLikesCount(rs.getInt("likes_count"));
                    
                    java.sql.Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                         note.setCreatedAt(ts.getTime()); 
                    }
                    notes.add(note);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return notes;
    }

    public static java.util.List<com.visiboard.pc.model.Note> getAllNotes() {
        java.util.List<com.visiboard.pc.model.Note> notes = new java.util.ArrayList<>();
        String query = "SELECT * FROM notes ORDER BY created_at DESC";
        try (Connection conn = getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(query);
             java.sql.ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                com.visiboard.pc.model.Note note = new com.visiboard.pc.model.Note();
                note.setNoteId(rs.getString("note_id"));
                note.setUserId(rs.getString("user_id"));
                note.setContent(rs.getString("content"));
                note.setImageUrl(rs.getString("image_url"));
                note.setLatitude(rs.getDouble("latitude"));
                note.setLongitude(rs.getDouble("longitude"));
                note.setLikesCount(rs.getInt("likes_count"));
                
                java.sql.Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) {
                        note.setCreatedAt(ts.getTime()); 
                }
                notes.add(note);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notes;
    }


    
    // Separate method for Warn if column missing, just log/notification for now
    public static void warnUser(String userId) {
         // In real impl, create notification. For now, just log.
         System.out.println("Warning user (DB Action placeholder): " + userId);
    }

    public static void deleteNote(String noteId) {
        String sql = "DELETE FROM notes WHERE note_id = ?";
        try (Connection conn = getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, noteId);
            pstmt.executeUpdate();
            System.out.println("Deleted note: " + noteId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void dismissReport(String reportId) {
        String sql = "DELETE FROM reports WHERE report_id = ?";
        try (Connection conn = getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, reportId);
            pstmt.executeUpdate();
            System.out.println("Dismissed (deleted) report: " + reportId);
        } catch (SQLException e) {
             e.printStackTrace();
        }
    }

    public static void deleteReport(String reportId) {
        dismissReport(reportId);
    }
}
