package com.visiboard.pc.controller;

import com.visiboard.pc.model.Note;
import com.visiboard.pc.service.ApiService; // Still used for basic fetch if needed, but we use DatabaseService for admin actions
import com.visiboard.pc.util.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

public class NoteDetailController {

    @FXML private javafx.scene.control.Button deleteButton;
    @FXML private ImageView userAvatar;
    @FXML private Label userNameLabel;
    @FXML private Label timestampLabel;
    @FXML private Label contentLabel;
    
    @FXML private javafx.scene.layout.StackPane imageContainer;
    @FXML private ImageView noteImageView;

    private Note note;
    private ApiService apiService;
    private Runnable onNoteDeleted;

    public void setNote(String noteId, ApiService apiService) {
        this.apiService = apiService;
        
        new Thread(() -> {
            java.util.List<Note> allNotes = com.visiboard.pc.services.DatabaseService.getAllNotes();
            Note target = null;
            for(Note n : allNotes) {
                if (n.getId().equals(noteId)) {
                    target = n;
                    break;
                }
            }
            
            if (target != null) {
                setNote(target);
            }
        }).start();
    }

    public void setNote(Note note) {
        this.note = note;
        
        new Thread(() -> {
            // Ensure User object is populated
            if (this.note.getUser() == null) {
                try {
                    java.util.List<com.visiboard.pc.model.User> users = com.visiboard.pc.services.DatabaseService.getAllUsers();
                    for(com.visiboard.pc.model.User u : users) {
                        if (u.getId().equals(this.note.getUserId())) {
                            this.note.setUser(u);
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching user for note: " + e.getMessage());
                }
            }
            
            javafx.application.Platform.runLater(this::updateUI);
        }).start();
    }
    
    public void setOnNoteDeleted(Runnable callback) {
        this.onNoteDeleted = callback;
    }

    private boolean fromReports = false;

    public void setReportContext(boolean fromReports) {
        this.fromReports = fromReports;
    }

    private void updateUI() {
        if (note == null) return;

        // User Info Logic
        String name = "Unknown User";
        String picUrl = null;
        
        if (note.getUser() != null) {
            name = note.getUser().getName();
            if (name == null || name.isEmpty()) name = note.getUser().getEmail();
            if (name == null || name.isEmpty()) name = "User " + note.getUserId();
            picUrl = note.getUser().getProfilePicUrl();
        } else {
             name = "User " + note.getUserId();
        }
        
        userNameLabel.setText(name);

        // Load image using cache service
        final String finalName = name;
        if (picUrl == null || picUrl.isEmpty()) {
            picUrl = "https://ui-avatars.com/api/?name=" + name.replace(" ", "+") + "&background=e94560&color=fff&size=48";
        }
        
        if (picUrl != null && !picUrl.isEmpty()) {
            final String finalPicUrl = picUrl;
            javafx.application.Platform.runLater(() -> {
                try {
                    javafx.scene.image.Image img;
                    if (finalPicUrl.startsWith("http")) {
                        img = new javafx.scene.image.Image(finalPicUrl, true);
                    } else {
                        String clean = finalPicUrl.contains(",") ? finalPicUrl.split(",")[1] : finalPicUrl;
                        clean = clean.replaceAll("\\s", "");
                        byte[] b = java.util.Base64.getDecoder().decode(clean);
                        img = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(b));
                    }
                    cropAndSetImage(userAvatar, img);
                } catch (Exception e) {
                   System.err.println("Error loading note avatar: " + e.getMessage());
                   // Fallback logic if needed
                }
            });
        }
            
        // Delete Button - Visible Only if NOT from Reports (Context Aware)
        // If fromReports is true -> Hide Delete Button (force user to use Report Card actions)
        if (fromReports) {
            deleteButton.setVisible(false);
            deleteButton.setManaged(false);
        } else {
            deleteButton.setVisible(true);
            deleteButton.setManaged(true);
        }

        contentLabel.setText(note.getContent());
        // Simple timestamp format
        if (note.getCreatedAt() > 0) {
             java.time.LocalDateTime date = java.time.Instant.ofEpochMilli(note.getCreatedAt())
                                        .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
             java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
             timestampLabel.setText(date.format(fmt));
             timestampLabel.setVisible(true);
        } else {
            timestampLabel.setVisible(false);
        }
        
        // Handle Note Image
        String base64Image = note.getImageUrl();
        if (base64Image != null && !base64Image.isEmpty()) {
            imageContainer.setVisible(true);
            imageContainer.setManaged(true);
            
            try {
                // Determine if URL or Base64
                if (base64Image.startsWith("http")) {
                    // It's a URL
                    noteImageView.setImage(new javafx.scene.image.Image(base64Image));
                } else {
                    // It's Base64
                    String cleanBase64 = base64Image;
                    if (base64Image.contains(",")) {
                        String[] parts = base64Image.split(",", 2);
                        if (parts.length > 1) {
                             cleanBase64 = parts[1];
                        }
                    }
                    cleanBase64 = cleanBase64.replaceAll("\\s", "");
                    byte[] imageBytes = java.util.Base64.getDecoder().decode(cleanBase64);
                    noteImageView.setImage(new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imageBytes)));
                }
                noteImageView.setFitWidth(550); 
            } catch (Exception e) {
                e.printStackTrace();
                imageContainer.setVisible(false);
                imageContainer.setManaged(false);
            }
        } else {
            imageContainer.setVisible(false);
            imageContainer.setManaged(false);
        }
    }
    
    private void cropAndSetImage(ImageView imageView, javafx.scene.image.Image image) {
        if (image == null) return;
        double width = image.getWidth();
        double height = image.getHeight();
        double size = Math.min(width, height);
        double x = (width - size) / 2.0;
        double y = (height - size) / 2.0;
        imageView.setViewport(new javafx.geometry.Rectangle2D(x, y, size, size));
        imageView.setImage(image);
        
        // Apply circle clip if not already
        if (imageView.getClip() == null) {
             Circle clip = new Circle(24, 24, 24); // Assuming 48x48
             imageView.setClip(clip);
        }
    }

    @FXML
    private void handleDelete() {
        if (note == null) return;
        
        String noteId = note.getId();
        String userId = note.getUserId();
        
        System.out.println("[Delete] Deleting note: " + noteId);
        
        new Thread(() -> {
            // 1. Delete from Firebase
            com.visiboard.pc.services.FirebaseService.deleteNote(noteId);
            
            // 2. Notify Note Owner via Firebase
            // 2. Notify Note Owner via Firebase
            if (userId != null && !userId.isEmpty()) {
                // Use the centralized notifyUser which now syncs to Firebase with correct schema
                com.visiboard.pc.services.DatabaseService.notifyUser(userId, 
                    "Your note has been deleted for violating community guidelines.", 
                    "admin_alert");
            }
            
            // 3. Delete Locally
            com.visiboard.pc.services.DatabaseService.deleteNote(noteId);
            
            // 4. UI Update
            javafx.application.Platform.runLater(() -> {
                 javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                 alert.setTitle("Admin Action");
                 alert.setHeaderText("Note Deleted");
                 alert.setContentText("Note has been deleted from Firebase and Local DB.");
                 alert.showAndWait();
                 
                 if (onNoteDeleted != null) {
                    onNoteDeleted.run();
                 }
                 deleteButton.getScene().getWindow().hide();
            });
        }).start();
    }
}
