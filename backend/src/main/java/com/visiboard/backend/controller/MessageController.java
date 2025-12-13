package com.visiboard.backend.controller;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.visiboard.backend.model.User;
import com.visiboard.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final UserRepository userRepository;

    public MessageController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> payload) {
        try {
            String fromUserId = (String) payload.get("fromUserId"); // Firebase UID
            String toUserId = (String) payload.get("toUserId");     // Firebase UID
            String messageText = (String) payload.get("messageText");
            Boolean isAnonymous = (Boolean) payload.get("isAnonymous");

            if (fromUserId == null || toUserId == null || messageText == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
            }

            User sender = userRepository.findByFirebaseUid(fromUserId);
            if (sender == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Sender not found"));
            }

            // Prepare Message Data for Firestore
            Map<String, Object> message = new HashMap<>();
            message.put("fromUserId", fromUserId);
            message.put("fromUserName", isAnonymous != null && isAnonymous ? "Anonymous" : sender.getName());
            message.put("fromUserProfilePic", isAnonymous != null && isAnonymous ? null : sender.getProfilePicUrl());
            message.put("toUserId", toUserId);
            message.put("messageText", messageText);
            message.put("timestamp", System.currentTimeMillis());
            message.put("anonymous", isAnonymous != null ? isAnonymous : false);
            message.put("read", false);

            Firestore db = FirestoreClient.getFirestore();
            if (db == null) {
                return ResponseEntity.internalServerError().body(Map.of("error", "Firebase not initialized"));
            }

            // Write to 'messages' collection
            com.google.api.core.ApiFuture<com.google.cloud.firestore.DocumentReference> future = 
                db.collection("messages").add(message);
            
            String messageId = future.get().getId();

            // Create Notification
            createMessageNotification(db, toUserId, fromUserId, 
                isAnonymous != null && isAnonymous ? "Anonymous" : sender.getName(),
                isAnonymous != null && isAnonymous ? null : sender.getProfilePicUrl(),
                messageText, messageId);

            return ResponseEntity.ok(Map.of("status", "success", "messageId", messageId));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private void createMessageNotification(Firestore db, String toUserId, String fromUserId, 
                                         String fromUserName, String fromUserProfilePic,
                                         String messageText, String messageId) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("toUserId", toUserId);
        notification.put("fromUserId", fromUserId);
        notification.put("fromUserName", fromUserName);
        notification.put("fromUserProfilePic", fromUserProfilePic);
        notification.put("type", "message"); // Lowercase to match Android
        notification.put("messageId", messageId);
        notification.put("messageText", messageText);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);
        
        db.collection("notifications").add(notification);
    }
}
