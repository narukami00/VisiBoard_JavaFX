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
            
            // Load image using cache service
            final String finalName = name;
            String imageUrl = picUrl;
            if (imageUrl == null || imageUrl.isEmpty()) {
                // Use generated avatar
                imageUrl = "https://ui-avatars.com/api/?name=" + name.replace(" ", "+") + "&background=e94560&color=fff&size=48";
            }
            
            System.out.println("[Image] Loading image for: " + name + " from: " + imageUrl);
            
            String finalImageUrl = imageUrl;
            com.visiboard.pc.service.ImageCacheService.getInstance()
                .getImage(imageUrl)
                .thenAccept(image -> {
                    javafx.application.Platform.runLater(() -> {
                        if (image != null) {
                            userAvatar.setImage(image);
                        } else {
                            // Fallback to generated avatar
                            String fallbackUrl = "https://ui-avatars.com/api/?name=" + 
                                finalName.substring(0, 1) + "&background=e94560&color=fff&size=48";
                            com.visiboard.pc.service.ImageCacheService.getInstance()
                                .getImage(fallbackUrl)
                                .thenAccept(fallbackImage -> {
                                    javafx.application.Platform.runLater(() -> {
                                        if (fallbackImage != null) {
                                            userAvatar.setImage(fallbackImage);
                                        }
                                    });
                                });
                        }
                    });
                })
                .exceptionally(e -> {
                    System.err.println("Error loading profile image: " + e.getMessage());
                    return null;
                });
            
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
        
        String userName = "Anonymous";
        if (comment.getUser() != null) {
            if (comment.getUser().getName() != null && !comment.getUser().getName().isEmpty()) {
                userName = comment.getUser().getName();
            } else if (comment.getUser().getEmail() != null) {
                userName = comment.getUser().getEmail().split("@")[0];
            }
        }
        
        String picUrl = (comment.getUser() != null) ? comment.getUser().getProfilePicUrl() : null;
        
        if (picUrl != null && !picUrl.isEmpty()) {
            final String finalUserName = userName;
            com.visiboard.pc.service.ImageCacheService.getInstance()
                .getImage(picUrl)
                .thenAccept(image -> {
                    javafx.application.Platform.runLater(() -> {
                        if (image != null) {
                            avatar.setImage(image);
                        } else {
                            setDefaultAvatar(avatar, finalUserName);
                        }
                    });
                })
                .exceptionally(e -> {
                    javafx.application.Platform.runLater(() -> setDefaultAvatar(avatar, finalUserName));
                    return null;
                });
        } else {
            setDefaultAvatar(avatar, userName);
        }
        
        VBox contentBox = new VBox(4);
        
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
        com.visiboard.pc.service.ImageCacheService.getInstance()
            .getImage(url)
            .thenAccept(image -> {
                if (image != null) {
                    javafx.application.Platform.runLater(() -> avatar.setImage(image));
                }
            });
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
        
        System.out.println("[Comment] Posting comment for note: " + note.getId());
        apiService.postComment(note.getId().toString(), text.trim()).thenAccept(newComment -> {
            if (newComment != null) {
                System.out.println("[Comment] Posted successfully");
                javafx.application.Platform.runLater(() -> {
                    commentInput.clear();
                    // Refresh note data to get updated comment count
                    apiService.getNoteById(note.getId().toString()).thenAccept(updatedNote -> {
                        if (updatedNote != null) {
                            note = updatedNote;
                            javafx.application.Platform.runLater(() -> {
                                updateUI();
                                loadComments();
                            });
                        }
                    });
                });
            }
        }).exceptionally(e -> {
            System.err.println("[Comment] Error posting: " + e.getMessage());
            e.printStackTrace();
            return null;
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
