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
        return noteRepository.findAll();
    }

    @PostMapping
    public Note createNote(@RequestBody Note note) {
        // For demo/PC, assign to the first found user or a specific one
        // In production, get ID from SecurityContext
        com.visiboard.backend.model.User user = userRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No users found! Create a user first."));
        
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
                            .map(like -> like.getUser().getEmail())
                            .collect(java.util.stream.Collectors.toList());
                    note.setLikedByUsers(likers);
                    
                    return org.springframework.http.ResponseEntity.ok(note);
                })
                .orElse(org.springframework.http.ResponseEntity.notFound().build());
    }
    @DeleteMapping("/{id}")
    public void deleteNote(@PathVariable java.util.UUID id) {
        noteRepository.deleteById(id);
    }

    @PostMapping("/{id}/like")
    public Note toggleLike(@PathVariable java.util.UUID id, @RequestParam(required = false) java.util.UUID userId) {
        Note note = noteRepository.findById(id).orElseThrow(() -> new RuntimeException("Note not found"));
        
        // For demo, use the first user or specific demo user
        com.visiboard.backend.model.User user = userRepository.findByEmail("demo@account.com").orElse(null);
        if (user == null) {
             user = userRepository.findAll().stream().findFirst().orElseThrow(() -> new RuntimeException("No user found"));
        }
        
        java.util.Optional<com.visiboard.backend.model.NoteLike> existingLike = noteLikeRepository.findByUserIdAndNoteId(user.getId(), note.getId());
        
        if (existingLike.isPresent()) {
            // Unlike
            noteLikeRepository.delete(existingLike.get());
            note.setLikesCount(Math.max(0, note.getLikesCount() - 1));
        } else {
            // Like
            com.visiboard.backend.model.NoteLike newLike = new com.visiboard.backend.model.NoteLike(user, note);
            noteLikeRepository.save(newLike);
            note.setLikesCount(note.getLikesCount() + 1);
        }
        
        Note savedNote = noteRepository.save(note);
        
        // Re-populate likedByUsers for response
        java.util.List<String> likers = noteLikeRepository.findByNoteId(savedNote.getId())
                .stream()
                .map(like -> like.getUser().getEmail())
                .collect(java.util.stream.Collectors.toList());
        savedNote.setLikedByUsers(likers);
        
        return savedNote;
    }
}
