package com.visiboard.pc.controller;

import com.visiboard.pc.model.Note;
import com.visiboard.pc.service.ApiService;
import com.visiboard.pc.util.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

public class NoteDetailController {

    @FXML private javafx.scene.control.Button likeButton;
    @FXML private javafx.scene.control.Button deleteButton;
    @FXML private ImageView userAvatar;
    @FXML private Label userNameLabel;
    @FXML private Label timestampLabel;
    @FXML private Label contentLabel;
    @FXML private Label likesCountLabel;
    @FXML private Label commentsCountLabel;
    @FXML private VBox commentsContainer;
    @FXML private TextArea commentInput;

    private Note note;
    private ApiService apiService;
    private Runnable onNoteDeleted;

    public void setNote(String noteId, ApiService apiService) {
        this.apiService = apiService;
        
        apiService.getNoteById(noteId).thenAccept(fetchedNote -> {
            this.note = fetchedNote;
            javafx.application.Platform.runLater(this::updateUI);
            loadComments();
        }).exceptionally(e -> {
            e.printStackTrace();
            return null;
        });
    }
    
    public void setOnNoteDeleted(Runnable callback) {
        this.onNoteDeleted = callback;
    }

    private void updateUI() {
        if (note == null) return;

        // User Info Logic
        if (note.getUser() != null) {
            String name = note.getUser().getName();
            if (name == null || name.isEmpty()) {
                if (note.getUser().getEmail() != null) {
                    name = note.getUser().getEmail().split("@")[0];
                } else {
                    name = "Anonymous";
                }
            }
            userNameLabel.setText(name);

            String picUrl = note.getUser().getProfilePicUrl();
            
            // Load image - support both URLs and base64 data URIs
            final String finalName = name;
            try {
                javafx.scene.image.Image image;
                
                if (picUrl != null && picUrl.startsWith("data:image/")) {
                    // Base64 data URI - extract and decode
                    System.out.println("[Image] Loading base64 image for: " + name);
                    String base64Data = picUrl.substring(picUrl.indexOf(",") + 1);
                    byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
                    image = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imageBytes));
                } else if (picUrl != null && !picUrl.isEmpty()) {
                    // Regular URL
                    System.out.println("[Image] Loading from URL for: " + name);
                    image = new javafx.scene.image.Image(picUrl, true);
                } else {
                    // Use generated avatar
                    System.out.println("[Image] Using generated avatar for: " + name);
                    String avatarUrl = "https://ui-avatars.com/api/?name=" + name.replace(" ", "+") + "&background=e94560&color=fff&size=48";
                    image = new javafx.scene.image.Image(avatarUrl, true);
                }
                
                image.errorProperty().addListener((obs, oldError, newError) -> {
                    if (newError) {
                        System.out.println("[Image] Error loading, using fallback for: " + finalName);
                        String fallbackUrl = "https://ui-avatars.com/api/?name=\" + finalName.substring(0, 1) + \"&background=e94560&color=fff&size=48";
                        userAvatar.setImage(new javafx.scene.image.Image(fallbackUrl, true));
                    }
                });
                userAvatar.setImage(image);
            } catch (Exception e) {
                System.err.println("Error loading profile image: " + e.getMessage());
                String fallbackUrl = "https://ui-avatars.com/api/?name=" + finalName.substring(0, 1) + "&background=e94560&color=fff&size=48";
                userAvatar.setImage(new javafx.scene.image.Image(fallbackUrl, true));
            }
            
            // Show delete button if current user owns the note
            String currentUserEmail = UserSession.getInstance().getUserEmail();
            boolean isOwner = note.getUser().getEmail() != null && 
                              currentUserEmail != null &&
                              note.getUser().getEmail().equalsIgnoreCase(currentUserEmail);
            
            System.out.println("Note owner email: " + note.getUser().getEmail());
            System.out.println("Current user email: " + currentUserEmail);
            System.out.println("Is owner: " + isOwner);
            
            if (isOwner) {
                deleteButton.setVisible(true);
            } else {
                deleteButton.setVisible(false);
            }
        } else {
            userNameLabel.setText("Unknown User");
        }

        contentLabel.setText(note.getContent());
        timestampLabel.setVisible(false);
        
        // Update like button
        if (note.getLikesCount() > 0) {
            likesCountLabel.setText(note.getLikesCount() + " likes");
            likeButton.setText("Liked");
            likeButton.getStyleClass().remove("modern-button");
            likeButton.getStyleClass().add("modern-button-liked");
        } else {
            likesCountLabel.setText("0 likes");
            likeButton.setText("Like");
            likeButton.getStyleClass().remove("modern-button-liked");
            if (!likeButton.getStyleClass().contains("modern-button")) {
                likeButton.getStyleClass().add("modern-button");
            }
        }
        
        // Update comments count
        if (note.getCommentsCount() > 0) {
            commentsCountLabel.setText(note.getCommentsCount() + " comments");
        } else {
            commentsCountLabel.setText("0 comments");
        }
    }
    
    private void loadComments() {
        if (note == null) {
            System.out.println("[Comments] Cannot load - note is null");
            return;
        }
        
        System.out.println("[Comments] Loading for note: " + note.getId());
        
        apiService.getComments(note.getId().toString()).thenAccept(comments -> {
            System.out.println("[Comments] Received: " + (comments != null ? comments.size() : "null"));
            if (comments != null && !comments.isEmpty()) {
                for (int i = 0; i < comments.size(); i++) {
                    com.visiboard.pc.model.Comment c = comments.get(i);
                    System.out.println("[Comments] #" + i + ": user=" + (c.getUser() != null ? c.getUser().getName() : "null") + ", content=" + c.getContent());
                }
            }
            javafx.application.Platform.runLater(() -> {
                commentsContainer.getChildren().clear();
                if (comments != null && !comments.isEmpty()) {
                    System.out.println("[Comments] Adding " + comments.size() + " to UI");
                    comments.forEach(this::addCommentToUI);
                    System.out.println("[Comments] UI has " + commentsContainer.getChildren().size() + " children");
                } else {
                    System.out.println("[Comments] None to display");
                }
            });
        }).exceptionally(e -> {
            System.err.println("[Comments] Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        });
    }
    
    private void addCommentToUI(com.visiboard.pc.model.Comment comment) {
        if (comment == null) return;
        
        HBox commentBox = new HBox(10);
        commentBox.setStyle("-fx-padding: 12; -fx-background-color: #f5f7fa; -fx-background-radius: 12;");
        
        ImageView avatar = new ImageView();
        avatar.setFitWidth(32);
        avatar.setFitHeight(32);
        Circle clip = new Circle(16, 16, 16);
        avatar.setClip(clip);
        
        if (comment.getUser() != null && comment.getUser().getProfilePicUrl() != null) {
            try {
                avatar.setImage(new javafx.scene.image.Image(comment.getUser().getProfilePicUrl(), true));
            } catch (Exception e) {
                setDefaultAvatar(avatar, comment.getUser().getName());
            }
        } else {
            setDefaultAvatar(avatar, comment.getUser() != null ? comment.getUser().getName() : "?");
        }
        
        VBox contentBox = new VBox(4);
        
        String userName = "Anonymous";
        if (comment.getUser() != null) {
            if (comment.getUser().getName() != null && !comment.getUser().getName().isEmpty()) {
                userName = comment.getUser().getName();
            } else if (comment.getUser().getEmail() != null) {
                userName = comment.getUser().getEmail().split("@")[0];
            }
        }
        
        Label nameLabel = new Label(userName);
        nameLabel.setStyle("-fx-font-weight: bold;");
        
        Label commentLabel = new Label(comment.getContent() != null ? comment.getContent() : "");
        commentLabel.setWrapText(true);
        commentLabel.setMaxWidth(300);
        
        contentBox.getChildren().addAll(nameLabel, commentLabel);
        commentBox.getChildren().addAll(avatar, contentBox);
        commentsContainer.getChildren().add(commentBox);
    }
    
    private void setDefaultAvatar(ImageView avatar, String name) {
        String initial = (name != null && !name.isEmpty()) ? name.substring(0, 1).toUpperCase() : "?";
        String url = "https://ui-avatars.com/api/?name=" + initial + "&background=e94560&color=fff&size=64";
        avatar.setImage(new javafx.scene.image.Image(url, true));
    }

    @FXML
    private void handleLike() {
        if (note == null) return;
        apiService.toggleLike(note.getId().toString()).thenAccept(updatedNote -> {
            if (updatedNote != null) {
                this.note = updatedNote;
                javafx.application.Platform.runLater(this::updateUI);
            }
        });
    }

    @FXML
    private void handlePostComment() {
        String text = commentInput.getText();
        if (text == null || text.trim().isEmpty() || note == null) return;
        
        apiService.postComment(note.getId().toString(), text.trim()).thenAccept(newComment -> {
            if (newComment != null) {
                javafx.application.Platform.runLater(() -> {
                    commentInput.clear();
                    loadComments();
                });
            }
        });
    }

    @FXML
    private void handleDelete() {
        if (note == null) return;
        
        System.out.println("[Delete] Deleting note: " + note.getId());
        apiService.deleteNote(note.getId().toString()).thenAccept(v -> {
            javafx.application.Platform.runLater(() -> {
                System.out.println("[Delete] Success");
                
                if (onNoteDeleted != null) {
                    System.out.println("[Delete] Calling map refresh callback");
                    onNoteDeleted.run();
                }
                
                deleteButton.getScene().getWindow().hide();
            });
        }).exceptionally(e -> {
            System.err.println("[Delete] Error: " + e.getMessage());
            return null;
        });
    }
}
