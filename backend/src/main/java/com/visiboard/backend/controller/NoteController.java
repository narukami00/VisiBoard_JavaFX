package com.visiboard.backend.controller;

import com.visiboard.backend.model.Note;
import com.visiboard.backend.repository.NoteRepository;
import com.visiboard.backend.service.SyncService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteRepository noteRepository;
    private final com.visiboard.backend.repository.UserRepository userRepository;
    private final SyncService syncService;

    private final com.visiboard.backend.repository.NoteLikeRepository noteLikeRepository;

    public NoteController(NoteRepository noteRepository, com.visiboard.backend.repository.UserRepository userRepository, SyncService syncService, com.visiboard.backend.repository.NoteLikeRepository noteLikeRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.syncService = syncService;
        this.noteLikeRepository = noteLikeRepository;
    }

    @GetMapping
    public List<Note> getAllNotes() {
        List<Note> notes = noteRepository.findAll();
        // Populate likedByUsers for UI
        for (Note note : notes) {
            java.util.List<String> likers = noteLikeRepository.findByNoteId(note.getId())
                    .stream()
                    .map(like -> like.getUser().getFirebaseUid())
                    .collect(java.util.stream.Collectors.toList());
            note.setLikedByUsers(likers);
        }
        return notes;
    }

    @PostMapping
    public Note createNote(@RequestBody Note note, @RequestParam(required = false) String userEmail) {
        com.visiboard.backend.model.User user;
        
        if (userEmail != null && !userEmail.isEmpty()) {
            System.out.println("Creating note for user: " + userEmail);
            user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));
        } else {
            // Fallback: assign to the first found user
            System.out.println("No user email provided, using first user");
            user = userRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("No users found! Create a user first."));
        }
        
        note.setUser(user);
        
        // Ensure Geometry is set if lat/lng are provided via transient fields
        if (note.getLat() != null && note.getLng() != null) {
            note.setLat(note.getLat()); // Triggers the setLat logic to create Point
        }

        Note savedNote = noteRepository.save(note);
        syncService.syncNoteToFirebase(savedNote);
        return savedNote;
    }
    @GetMapping("/{id}")
    public org.springframework.http.ResponseEntity<Note> getNoteById(@PathVariable java.util.UUID id) {
        return noteRepository.findById(id)
                .map(note -> {
                    // Self-healing: Fix missing user name
                    if (note.getUser() != null && (note.getUser().getName() == null || note.getUser().getName().isEmpty())) {
                        String email = note.getUser().getEmail();
                        String newName = (email != null && email.contains("@")) ? email.split("@")[0] : "Demo User";
                        note.getUser().setName(newName);
                        // If profile pic is missing, set a default one
                        if (note.getUser().getProfilePicUrl() == null || note.getUser().getProfilePicUrl().isEmpty()) {
                            note.getUser().setProfilePicUrl("https://ui-avatars.com/api/?name=" + newName + "&background=random");
                        }
                        userRepository.save(note.getUser());
                    }
                    
                    // Populate likedByUsers
                    java.util.List<String> likers = noteLikeRepository.findByNoteId(note.getId())
                            .stream()
                            .map(like -> like.getUser().getFirebaseUid())
                            .collect(java.util.stream.Collectors.toList());
                    note.setLikedByUsers(likers);
                    
                    return org.springframework.http.ResponseEntity.ok(note);
                })
                .orElse(org.springframework.http.ResponseEntity.notFound().build());
    }
    @DeleteMapping("/{id}")
    public org.springframework.http.ResponseEntity<Void> deleteNote(@PathVariable java.util.UUID id) {
        Note note = noteRepository.findById(id).orElse(null);
        if (note == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        
        String firebaseId = note.getFirebaseId();
        
        noteRepository.deleteById(id);
        
        if (firebaseId != null && !firebaseId.isEmpty()) {
            syncService.deleteNoteFromFirebase(firebaseId);
        } else {
            // Fallback: try to delete using UUID if firebaseId was missing (legacy notes)
            syncService.deleteNoteFromFirebase(id.toString());
        }
        
        return org.springframework.http.ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/like")
    public Note toggleLike(@PathVariable java.util.UUID id, @RequestParam(required = false) java.util.UUID userId) {
        Note note = noteRepository.findById(id).orElseThrow(() -> new RuntimeException("Note not found"));
        
        com.visiboard.backend.model.User user = null;
        
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }
        
        if (user == null) {
            // Fallback: For demo, use the first user or specific demo user
            user = userRepository.findByEmail("demo@account.com").orElse(null);
            if (user == null) {
                 user = userRepository.findAll().stream().findFirst().orElseThrow(() -> new RuntimeException("No user found"));
            }
        }
        
        java.util.Optional<com.visiboard.backend.model.NoteLike> existingLike = noteLikeRepository.findByUserIdAndNoteId(user.getId(), note.getId());
        boolean isLiked = false;
        
        if (existingLike.isPresent()) {
            // Unlike
            noteLikeRepository.delete(existingLike.get());
            note.setLikesCount(Math.max(0, note.getLikesCount() - 1));
        } else {
            // Like
            com.visiboard.backend.model.NoteLike newLike = new com.visiboard.backend.model.NoteLike(user, note);
            noteLikeRepository.save(newLike);
            note.setLikesCount(note.getLikesCount() + 1);
            isLiked = true;
        }
        
        Note savedNote = noteRepository.save(note);
        
        // Re-populate likedByUsers for response AND sync
        java.util.List<String> likers = noteLikeRepository.findByNoteId(savedNote.getId())
                .stream()
                .map(like -> like.getUser().getFirebaseUid())
                .collect(java.util.stream.Collectors.toList());
        savedNote.setLikedByUsers(likers);
        
        // Sync updated like count and likedBy users to Firebase ATOMICALLY
        // This prevents overwriting the list with partial local data
        syncService.updateNoteLikeInFirebase(savedNote.getId().toString(), user.getFirebaseUid(), isLiked);
        
        return savedNote;
    }
}
