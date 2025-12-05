package com.visiboard.backend.controller;

import com.visiboard.backend.model.User;
import com.visiboard.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final UserRepository userRepository;
    
    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Login endpoint
     * For demo purposes, just checks if user exists by email
     * In production, implement proper password hashing and verification
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");
        
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        
        // Find user by email
        java.util.Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            return ResponseEntity.ok(userOptional.get());
        } else {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }
    
    /**
     * Signup endpoint
     * Creates a new user
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> userData) {
        String email = userData.get("email");
        String password = userData.get("password");
        String name = userData.get("name");
        
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        
        // Check if user already exists
        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User already exists"));
        }
        
        // Create new user
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setName(name != null && !name.isEmpty() ? name : email.split("@")[0]);
        newUser.setProfilePicUrl("https://ui-avatars.com/api/?name=" + newUser.getName().replace(" ", "+") + "&background=e94560&color=fff");
        
        User savedUser = userRepository.save(newUser);
        return ResponseEntity.ok(savedUser);
    }
}
