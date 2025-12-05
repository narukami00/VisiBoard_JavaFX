package com.visiboard.pc.service;

import javafx.scene.image.Image;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.sql.*;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class ImageCacheService {
    
    private static ImageCacheService instance;
    private Connection connection;
    
    private ImageCacheService() {
        initDatabase();
    }
    
    public static ImageCacheService getInstance() {
        if (instance == null) {
            instance = new ImageCacheService();
        }
        return instance;
    }
    
    private void initDatabase() {
        try {
            String dbPath = System.getProperty("user.home") + "/.visiboard/image_cache.db";
            java.io.File dbDir = new java.io.File(System.getProperty("user.home") + "/.visiboard");
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }
            
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            
            String createTable = "CREATE TABLE IF NOT EXISTS image_cache (" +
                                "url TEXT PRIMARY KEY," +
                                "image_data BLOB," +
                                "cached_at INTEGER" +
                                ")";
            
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createTable);
            }
            
            System.out.println("[ImageCache] Database initialized at: " + dbPath);
        } catch (SQLException e) {
            System.err.println("[ImageCache] Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public CompletableFuture<Image> getImage(String url) {
        if (url == null || url.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        
        // Handle base64 data URIs directly
        if (url.startsWith("data:image/")) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    String base64Data = url.substring(url.indexOf(",") + 1);
                    // Use MimeDecoder to handle newlines and potential whitespace
                    byte[] imageBytes = Base64.getMimeDecoder().decode(base64Data);
                    return new Image(new ByteArrayInputStream(imageBytes));
                } catch (Exception e) {
                    System.err.println("[ImageCache] Failed to decode base64 image: " + e.getMessage());
                    return null;
                }
            });
        }
        
        // Handle raw base64 strings without the data URI prefix
        // Check for common image base64 signatures: /9j/ (JPEG), iVBOR (PNG), R0lGOD (GIF)
        if (!url.startsWith("http://") && !url.startsWith("https://") && 
            (url.startsWith("/9j/") || url.startsWith("iVBOR") || url.startsWith("R0lGOD") || 
             (url.length() > 100 && url.matches("^[A-Za-z0-9+/=\\s]+$")))) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    System.out.println("[ImageCache] Detected raw base64 string (length: " + url.length() + "), attempting to decode...");
                    // Remove any whitespace
                    // Remove any whitespace just in case, though MimeDecoder handles most
                    String cleanBase64 = url.replaceAll("\\s", "");
                    byte[] imageBytes = Base64.getMimeDecoder().decode(cleanBase64);
                    System.out.println("[ImageCache] Successfully decoded " + imageBytes.length + " bytes");
                    return new Image(new ByteArrayInputStream(imageBytes));
                } catch (Exception e) {
                    System.err.println("[ImageCache] Failed to decode raw base64: " + e.getMessage());
                    return null;
                }
            });
        }
        
        return CompletableFuture.supplyAsync(() -> {
            // Try to get from cache first
            Image cachedImage = getFromCache(url);
            if (cachedImage != null) {
                System.out.println("[ImageCache] Loaded from cache: " + url);
                return cachedImage;
            }
            
            // Download and cache
            System.out.println("[ImageCache] Downloading: " + url);
            return downloadAndCache(url);
        });
    }
    
    private Image getFromCache(String url) {
        try {
            String query = "SELECT image_data FROM image_cache WHERE url = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, url);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    byte[] imageData = rs.getBytes("image_data");
                    if (imageData != null) {
                        return new Image(new ByteArrayInputStream(imageData));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[ImageCache] Error reading from cache: " + e.getMessage());
        }
        return null;
    }
    
    private Image downloadAndCache(String urlString) {
        try {
            URL url = new URL(urlString);
            InputStream inputStream = url.openStream();
            
            // Read image data
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[1024];
            int nRead;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] imageData = buffer.toByteArray();
            inputStream.close();
            
            // Cache the image
            cacheImage(urlString, imageData);
            
            // Return the image
            return new Image(new ByteArrayInputStream(imageData));
            
        } catch (Exception e) {
            System.err.println("[ImageCache] Failed to download image: " + e.getMessage());
            return null;
        }
    }
    
    private void cacheImage(String url, byte[] imageData) {
        try {
            String insert = "INSERT OR REPLACE INTO image_cache (url, image_data, cached_at) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(insert)) {
                pstmt.setString(1, url);
                pstmt.setBytes(2, imageData);
                pstmt.setLong(3, System.currentTimeMillis());
                pstmt.executeUpdate();
            }
            System.out.println("[ImageCache] Cached image: " + url);
        } catch (SQLException e) {
            System.err.println("[ImageCache] Failed to cache image: " + e.getMessage());
        }
    }
    
    public void clearOldCache(long maxAgeMs) {
        try {
            long cutoffTime = System.currentTimeMillis() - maxAgeMs;
            String delete = "DELETE FROM image_cache WHERE cached_at < ?";
            try (PreparedStatement pstmt = connection.prepareStatement(delete)) {
                pstmt.setLong(1, cutoffTime);
                int deleted = pstmt.executeUpdate();
                System.out.println("[ImageCache] Cleared " + deleted + " old images");
            }
        } catch (SQLException e) {
            System.err.println("[ImageCache] Failed to clear old cache: " + e.getMessage());
        }
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("[ImageCache] Failed to close database: " + e.getMessage());
        }
    }
}
