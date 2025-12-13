package com.visiboard.backend.controller;

import com.visiboard.backend.model.User;
import com.visiboard.backend.repository.UserRepository;
import com.visiboard.backend.service.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;
import com.google.firebase.cloud.FirestoreClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/follow")
public class FollowController {

    private final UserRepository userRepository;
    
    public FollowController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/{followerUid}/{targetUid}")
    public ResponseEntity<?> followUser(@PathVariable String followerUid, @PathVariable String targetUid) {
        try {
            if (followerUid.equals(targetUid)) {
                return ResponseEntity.badRequest().body("Cannot follow yourself");
            }
            
            User follower = userRepository.findByFirebaseUid(followerUid);
            User target = userRepository.findByFirebaseUid(targetUid);
            
            if (follower == null || target == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Update Firestore
            // Path: users/{uid}/following/{targetUid}
            // Path: users/{targetUid}/followers/{uid}
            
            Firestore db = FirestoreClient.getFirestore();
            WriteBatch batch = db.batch();
            
            // 1. Add to follower's "following" subcollection
            Map<String, Object> followingData = new HashMap<>();
            followingData.put("followingId", targetUid);
            followingData.put("followingName", target.getName());
            
            // 2. Add to target's "followers" subcollection
            Map<String, Object> followerData = new HashMap<>();
            followerData.put("followerId", followerUid);
            followerData.put("followerName", follower.getName());
            
            batch.set(db.collection("users").document(followerUid).collection("following").document(targetUid), followingData);
            batch.set(db.collection("users").document(targetUid).collection("followers").document(followerUid), followerData);
            
            // Increment counts
            batch.update(db.collection("users").document(followerUid), "followingCount", com.google.cloud.firestore.FieldValue.increment(1));
            batch.update(db.collection("users").document(targetUid), "followersCount", com.google.cloud.firestore.FieldValue.increment(1));

            batch.commit().get();
            
            return ResponseEntity.ok().body(Map.of("message", "Followed successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error following user: " + e.getMessage());
        }
    }

    @PostMapping("/unfollow/{followerUid}/{targetUid}")
    public ResponseEntity<?> unfollowUser(@PathVariable String followerUid, @PathVariable String targetUid) {
        try {
             Firestore db = FirestoreClient.getFirestore();
             WriteBatch batch = db.batch();
             
             // Delete from collections
             batch.delete(db.collection("users").document(followerUid).collection("following").document(targetUid));
             batch.delete(db.collection("users").document(targetUid).collection("followers").document(followerUid));
             
             // Decrement counts
             batch.update(db.collection("users").document(followerUid), "followingCount", com.google.cloud.firestore.FieldValue.increment(-1));
             batch.update(db.collection("users").document(targetUid), "followersCount", com.google.cloud.firestore.FieldValue.increment(-1));
             
             batch.commit().get();
             
             return ResponseEntity.ok().body(Map.of("message", "Unfollowed successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error unfollowing: " + e.getMessage());
        }
    }

    @GetMapping("/check/{followerUid}/{targetUid}")
    public ResponseEntity<Map<String, Boolean>> isFollowing(@PathVariable String followerUid, @PathVariable String targetUid) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            // Check if document exists in users/{follower}/following/{target}
            boolean exists = db.collection("users").document(followerUid)
                               .collection("following").document(targetUid)
                               .get().get().exists();
            
            return ResponseEntity.ok(Map.of("isFollowing", exists));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
