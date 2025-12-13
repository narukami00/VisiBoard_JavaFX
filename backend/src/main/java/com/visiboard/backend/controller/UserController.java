package com.visiboard.backend.controller;

import com.visiboard.backend.model.User;
import com.visiboard.backend.repository.UserRepository;
import com.visiboard.backend.service.SyncService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final SyncService syncService;

    public UserController(UserRepository userRepository, SyncService syncService) {
        this.userRepository = userRepository;
        this.syncService = syncService;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        User savedUser = userRepository.save(user);
        syncService.syncUserToFirebase(savedUser);
        return savedUser;
    }
    @GetMapping("/{id}")
    public org.springframework.http.ResponseEntity<User> getUserByIdOrEmail(@PathVariable String id) {
        // If it looks like an email
        if (id.contains("@")) {
            return userRepository.findByEmail(id)
                    .map(org.springframework.http.ResponseEntity::ok)
                    .orElse(org.springframework.http.ResponseEntity.notFound().build());
        } 
        // Otherwise try as UUID
        else {
            try {
                java.util.UUID uuid = java.util.UUID.fromString(id);
                return userRepository.findById(uuid)
                        .map(org.springframework.http.ResponseEntity::ok)
                        .orElse(org.springframework.http.ResponseEntity.notFound().build());
            } catch (IllegalArgumentException e) {
                return org.springframework.http.ResponseEntity.badRequest().build();
            }
        }
    }

    @GetMapping("/firebase/{uid}")
    public org.springframework.http.ResponseEntity<User> getUserByFirebaseUid(@PathVariable String uid) {
        User user = userRepository.findByFirebaseUid(uid);
        if (user != null) {
            return org.springframework.http.ResponseEntity.ok(user);
        } else {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
    }
}
