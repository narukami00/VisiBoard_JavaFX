package com.visiboard.pc.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visiboard.pc.model.Note;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ApiService {

    private static final String BASE_URL = "http://localhost:8080/api";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ApiService() {
        this.httpClient = java.net.http.HttpClient.newBuilder()
                .executor(com.visiboard.pc.util.ConcurrencyManager.getExecutor())
                .version(java.net.http.HttpClient.Version.HTTP_2)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public CompletableFuture<List<Note>> getNotes() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/notes"))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("GET /notes Status: " + response.statusCode());
                    System.out.println("GET /notes Body: " + response.body());
                    if (response.statusCode() != 200) {
                        return Collections.<Note>emptyList();
                    }
                    try {
                        return objectMapper.readValue(response.body(), new TypeReference<List<Note>>() {});
                    } catch (Exception e) {
                        e.printStackTrace();
                        return Collections.<Note>emptyList();
                    }
                });
    }

    public java.util.concurrent.CompletableFuture<java.util.Map<String, Long>> getWeeklyEngagement() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/analytics/engagement"))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    try {
                        return objectMapper.readValue(body, new TypeReference<java.util.Map<String, Long>>() {});
                    } catch (Exception e) {
                        e.printStackTrace();
                        return java.util.Collections.emptyMap();
                    }
                });
    }

    public java.util.concurrent.CompletableFuture<com.visiboard.pc.model.User> getUserByEmail(String email) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/users/" + email))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    try {
                        return objectMapper.readValue(body, com.visiboard.pc.model.User.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                });
    }

    public java.util.concurrent.CompletableFuture<List<com.visiboard.pc.model.User>> getAllUsers() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/users"))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    try {
                        return objectMapper.readValue(body, new TypeReference<List<com.visiboard.pc.model.User>>() {});
                    } catch (Exception e) {
                        e.printStackTrace();
                        return Collections.emptyList();
                    }
                });
    }
    public java.util.concurrent.CompletableFuture<Note> getNoteById(String id) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/notes/" + id))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("GET /notes/" + id + " Status: " + response.statusCode());
                    System.out.println("GET /notes/" + id + " Body: " + response.body());
                    if (response.statusCode() != 200) {
                        return null;
                    }
                    try {
                        return objectMapper.readValue(response.body(), Note.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                });
    }

    public java.util.concurrent.CompletableFuture<Note> createNote(Note note) {
        return createNote(note, null);
    }
    
    public java.util.concurrent.CompletableFuture<Note> createNote(Note note, String userEmail) {
        try {
            String json = objectMapper.writeValueAsString(note);
            String url = BASE_URL + "/notes";
            if (userEmail != null && !userEmail.isEmpty()) {
                url += "?userEmail=" + java.net.URLEncoder.encode(userEmail, "UTF-8");
            }
            
            System.out.println("POST " + url);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        System.out.println("POST /notes Status: " + response.statusCode());
                        try {
                            return objectMapper.readValue(response.body(), Note.class);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
    }
    public java.util.concurrent.CompletableFuture<List<com.visiboard.pc.model.Comment>> getComments(String noteId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/comments/note/" + noteId))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("GET /comments/note/" + noteId + " Status: " + response.statusCode());
                    System.out.println("GET /comments/note/" + noteId + " Body: " + response.body());
                    if (response.statusCode() != 200) {
                        return Collections.<com.visiboard.pc.model.Comment>emptyList();
                    }
                    try {
                        List<com.visiboard.pc.model.Comment> comments = objectMapper.readValue(response.body(), new TypeReference<List<com.visiboard.pc.model.Comment>>() {});
                        System.out.println("Parsed " + comments.size() + " comments");
                        return comments;
                    } catch (Exception e) {
                        System.err.println("Error parsing comments: " + e.getMessage());
                        e.printStackTrace();
                        return Collections.<com.visiboard.pc.model.Comment>emptyList();
                    }
                });
    }

    public java.util.concurrent.CompletableFuture<com.visiboard.pc.model.Comment> postComment(String noteId, String content) {
        try {
            // Simple request object
            var payload = java.util.Map.of("noteId", noteId, "content", content);
            String json = objectMapper.writeValueAsString(payload);
            
            System.out.println("POST /comments with noteId: " + noteId + ", content: " + content);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/comments"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        System.out.println("POST /comments Status: " + response.statusCode());
                        System.out.println("POST /comments Body: " + response.body());
                        if (response.statusCode() != 200 && response.statusCode() != 201) {
                            return null;
                        }
                        try {
                            return objectMapper.readValue(response.body(), com.visiboard.pc.model.Comment.class);
                        } catch (Exception e) {
                            System.err.println("Error parsing comment response: " + e.getMessage());
                            e.printStackTrace();
                            return null;
                        }
                    });
        } catch (Exception e) {
            System.err.println("Error creating comment request: " + e.getMessage());
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
    }

    public java.util.concurrent.CompletableFuture<Note> toggleLike(String noteId) {
        String url = BASE_URL + "/notes/" + noteId + "/like";
        
        // Append current user ID if logged in
        // Append current user ID if logged in
        String userId = com.visiboard.pc.util.UserSession.getInstance().getUserId();
        if (userId != null) {
            url += "?userId=" + userId;
        }
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> {
                    try {
                        return objectMapper.readValue(body, Note.class);
                    } catch (Exception e) {
                        return null;
                    }
                });
    }

    public java.util.concurrent.CompletableFuture<Void> deleteNote(String noteId) {
        System.out.println("DELETE /notes/" + noteId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/notes/" + noteId))
                .DELETE()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("DELETE /notes/" + noteId + " Status: " + response.statusCode());
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        System.out.println("Note deleted successfully");
                    } else {
                        System.err.println("Failed to delete note. Response: " + response.body());
                    }
                    return null;
                });
    }
    
    /**
     * Authenticate user with email and password
     */
    public CompletableFuture<com.visiboard.pc.model.User> login(String email, String password) {
        try {
            java.util.Map<String, String> credentials = new java.util.HashMap<>();
            credentials.put("email", email);
            credentials.put("password", password);
            
            String requestBody = objectMapper.writeValueAsString(credentials);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        System.out.println("Login response: " + response.statusCode());
                        if (response.statusCode() == 200) {
                            try {
                                return objectMapper.readValue(response.body(), com.visiboard.pc.model.User.class);
                            } catch (Exception e) {
                                e.printStackTrace();
                                return null;
                            }
                        }
                        return null;
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * Register a new user
     */
    public CompletableFuture<com.visiboard.pc.model.User> signup(String email, String password, String name) {
        try {
            java.util.Map<String, String> userData = new java.util.HashMap<>();
            userData.put("email", email);
            userData.put("password", password);
            if (name != null && !name.isEmpty()) {
                userData.put("name", name);
            }
            
            String requestBody = objectMapper.writeValueAsString(userData);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/auth/signup"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        System.out.println("Signup response: " + response.statusCode());
                        if (response.statusCode() == 200) {
                            try {
                                return objectMapper.readValue(response.body(), com.visiboard.pc.model.User.class);
                            } catch (Exception e) {
                                e.printStackTrace();
                                return null;
                            }
                        }
                        return null;
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * Get notifications for a user
     */
    public CompletableFuture<List<com.visiboard.pc.model.Notification>> getNotifications(String userId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/notifications/user/" + userId))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("GET /notifications/user/" + userId + " Status: " + response.statusCode());
                    System.out.println("Response body: " + response.body());
                    if (response.statusCode() != 200) {
                        return Collections.<com.visiboard.pc.model.Notification>emptyList();
                    }
                    try {
                        return objectMapper.readValue(response.body(), new TypeReference<List<com.visiboard.pc.model.Notification>>() {});
                    } catch (Exception e) {
                        e.printStackTrace();
                        return Collections.<com.visiboard.pc.model.Notification>emptyList();
                    }
                });
    }

    public CompletableFuture<List<com.visiboard.pc.model.Notification>> getNotificationsByFirebaseUid(String firebaseUid) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/notifications/user/firebase/" + firebaseUid))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("GET /notifications/user/firebase/" + firebaseUid + " Status: " + response.statusCode());
                    System.out.println("Response body: " + response.body());
                    if (response.statusCode() != 200) {
                        return Collections.<com.visiboard.pc.model.Notification>emptyList();
                    }
                    try {
                        return objectMapper.readValue(response.body(), new TypeReference<List<com.visiboard.pc.model.Notification>>() {});
                    } catch (Exception e) {
                        e.printStackTrace();
                        return Collections.<com.visiboard.pc.model.Notification>emptyList();
                    }
                });
    }
    
    /**
     * Delete a notification
     */
    public CompletableFuture<Boolean> deleteNotification(String notificationId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/notifications/" + notificationId))
                .DELETE()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("DELETE /notifications/" + notificationId + " Status: " + response.statusCode());
                    return response.statusCode() >= 200 && response.statusCode() < 300;
                });
    }

    public CompletableFuture<Boolean> followUser(String targetUid) {
        return toggleFollow(targetUid, true);
    }
    
    public CompletableFuture<Boolean> unfollowUser(String targetUid) {
        return toggleFollow(targetUid, false);
    }
    
    private CompletableFuture<Boolean> toggleFollow(String targetUid, boolean isFollow) {
        try {
            com.visiboard.pc.model.User currentUser = com.visiboard.pc.util.UserSession.getInstance().getCurrentUser();
            if (currentUser == null || currentUser.getFirebaseUid() == null) return CompletableFuture.completedFuture(false);
            
            String endpoint = isFollow ? "/follow/" : "/follow/unfollow/";
            String url = BASE_URL + endpoint + currentUser.getFirebaseUid() + "/" + targetUid;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        System.out.println((isFollow ? "Follow" : "Unfollow") + " response: " + response.statusCode());
                        return response.statusCode() == 200;
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }
    
    public CompletableFuture<Boolean> isFollowing(String targetUid) {
        try {
            com.visiboard.pc.model.User currentUser = com.visiboard.pc.util.UserSession.getInstance().getCurrentUser();
            if (currentUser == null || currentUser.getFirebaseUid() == null) return CompletableFuture.completedFuture(false);
            
            String url = BASE_URL + "/follow/check/" + currentUser.getFirebaseUid() + "/" + targetUid;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                java.util.Map<String, Boolean> result = objectMapper.readValue(response.body(), new TypeReference<java.util.Map<String, Boolean>>() {});
                                return result.get("isFollowing");
                            } catch (Exception e) {
                                e.printStackTrace();
                                return false;
                            }
                        }
                        return false;
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }
    
    public CompletableFuture<com.visiboard.pc.model.User> getUserByFirebaseUid(String uid) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/users/firebase/" + uid))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("GET /users/firebase/" + uid + " Status: " + response.statusCode());
                    System.out.println("Body: " + response.body());
                    if (response.statusCode() != 200) return null;
                    try {
                        return objectMapper.readValue(response.body(), com.visiboard.pc.model.User.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                });
    }

    public CompletableFuture<com.visiboard.pc.model.User> getUserById(String uuid) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/users/" + uuid))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("GET /users/" + uuid + " Status: " + response.statusCode());
                    System.out.println("Body: " + response.body());
                    if (response.statusCode() != 200) return null;
                    try {
                        return objectMapper.readValue(response.body(), com.visiboard.pc.model.User.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                });
    }

    public CompletableFuture<Boolean> sendMessage(String recipientFirebaseUid, String messageText) {
        try {
            com.visiboard.pc.model.User currentUser = com.visiboard.pc.util.UserSession.getInstance().getCurrentUser();
            if (currentUser == null || currentUser.getFirebaseUid() == null) {
                return CompletableFuture.completedFuture(false);
            }

            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("fromUserId", currentUser.getFirebaseUid());
            payload.put("toUserId", recipientFirebaseUid);
            payload.put("messageText", messageText);
            payload.put("isAnonymous", false);

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/messages/send"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        System.out.println("Send Message response: " + response.statusCode());
                        return response.statusCode() == 200;
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }
}
