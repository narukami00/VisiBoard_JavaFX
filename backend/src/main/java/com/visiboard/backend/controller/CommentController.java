package com.visiboard.backend.controller;

import com.visiboard.backend.model.Comment;
import com.visiboard.backend.model.Note;
import com.visiboard.backend.model.User;
import com.visiboard.backend.repository.CommentRepository;
import com.visiboard.backend.repository.NoteRepository;
import com.visiboard.backend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentRepository commentRepository;
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    public CommentController(CommentRepository commentRepository, NoteRepository noteRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/note/{noteId}")
    public List<Comment> getCommentsByNoteId(@PathVariable UUID noteId) {
        return commentRepository.findByNoteIdOrderByCreatedAtDesc(noteId);
    }

    @PostMapping
    public Comment addComment(@RequestBody CommentRequest request) {
        Note note = noteRepository.findById(request.noteId)
                .orElseThrow(() -> new RuntimeException("Note not found"));
        
        // For demo, use first user if userId not provided
        User user;
        if (request.userId != null) {
            user = userRepository.findById(request.userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } else {
             user = userRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No users found"));
        }

        Comment comment = new Comment();
        comment.setNote(note);
        comment.setUser(user);
        comment.setContent(request.content);
        
        return commentRepository.save(comment);
    }

    public static class CommentRequest {
        public UUID noteId;
        public UUID userId;
        public String content;
    }
}
