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
    @GetMapping("/{email}")
    public org.springframework.http.ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        return userRepository.findByEmail(email)
                .map(org.springframework.http.ResponseEntity::ok)
                .orElse(org.springframework.http.ResponseEntity.notFound().build());
    }
}
