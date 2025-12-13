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
    
    @FXML private javafx.scene.layout.StackPane imageContainer;
    @FXML private ImageView noteImageView;
    @FXML private ImageView currentUserAvatar;

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
                            cropAndSetImage(userAvatar, image);
                        } else {
                            // Fallback to generated avatar
                            String fallbackUrl = "https://ui-avatars.com/api/?name=" + 
                                finalName.substring(0, 1) + "&background=e94560&color=fff&size=48";
                            com.visiboard.pc.service.ImageCacheService.getInstance()
                                .getImage(fallbackUrl)
                                .thenAccept(fallbackImage -> {
                                    javafx.application.Platform.runLater(() -> {
                                        if (fallbackImage != null) {
                                            cropAndSetImage(userAvatar, fallbackImage);
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
        
        // Update like button based on current user's like status
        String currentUserFirebaseUid = UserSession.getInstance().getFirebaseUid();
        boolean isLikedByCurrentUser = note.getLikedByUsers() != null && 
                                       currentUserFirebaseUid != null &&
                                       note.getLikedByUsers().contains(currentUserFirebaseUid);
        
        System.out.println("[Like] Current user Firebase UID: " + currentUserFirebaseUid);
        System.out.println("[Like] Note liked by users: " + note.getLikedByUsers());
        System.out.println("[Like] Is liked by current user: " + isLikedByCurrentUser);
        
        likesCountLabel.setText(note.getLikesCount() + " likes");
        
        // Clear all style classes first
        likeButton.getStyleClass().removeAll("modern-button", "modern-button-liked");
        
        if (isLikedByCurrentUser) {
            likeButton.setText("\u2764 Liked");
            likeButton.getStyleClass().add("modern-button-liked");
        } else {
            likeButton.setText("\u2661 Like");
            likeButton.getStyleClass().add("modern-button");
        }
        
        if (note.getCommentsCount() > 0) {
            commentsCountLabel.setText(note.getCommentsCount() + " comments");
        } else {
            commentsCountLabel.setText("0 comments");
        }
        
        // Handle Note Image
        String base64Image = note.getImageBase64();
        if (base64Image != null && !base64Image.isEmpty()) {
            System.out.println("[Image Debug] Found image data, length: " + base64Image.length());
            imageContainer.setVisible(true);
            imageContainer.setManaged(true);
            
            try {
                String cleanBase64 = base64Image;
                // Remove data:image/jpeg;base64, prefix if present
                if (base64Image.contains(",")) {
                    String[] parts = base64Image.split(",", 2);
                    if (parts.length > 1) {
                         cleanBase64 = parts[1];
                    }
                }
                
                // Clean any whitespace/newlines just in case
                cleanBase64 = cleanBase64.replaceAll("\\s", "");
                
                byte[] imageBytes = java.util.Base64.getDecoder().decode(cleanBase64);
                javafx.scene.image.Image image = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imageBytes));
                
                if (image.isError()) {
                    System.err.println("[Image Debug] Image loading error: " + image.getException());
                } else {
                    System.out.println("[Image Debug] Image decoded successfully: " + image.getWidth() + "x" + image.getHeight());
                }
                
                noteImageView.setImage(image);
                
                // Adjust width to fit container (approx 550px available width in 600px window)
                double maxWidth = 550;
                if (image.getWidth() > maxWidth) {
                    noteImageView.setFitWidth(maxWidth);
                } else {
                    noteImageView.setFitWidth(image.getWidth());
                }
                
            } catch (Exception e) {
                System.err.println("[Image] Error decoding base64 image: " + e.getMessage());
                e.printStackTrace();
                imageContainer.setVisible(false);
                imageContainer.setManaged(false);
            }
        } else {
            System.out.println("[Image Debug] No image data found in note.");
            imageContainer.setVisible(false);
            imageContainer.setManaged(false);
        }
        
        // Setup current user avatar in footer
        loadCurrentUserAvatar();
        
        // Add click handlers for profile
        if (note.getUser() != null && note.getUser().getFirebaseUid() != null) {
            String uid = note.getUser().getFirebaseUid();
            String uName = note.getUser().getName();
            
            javafx.event.EventHandler<javafx.scene.input.MouseEvent> openProfileHandler = e -> {
                System.out.println("[NoteDetail] User clicked profile: " + uid);
                javafx.application.Platform.runLater(() -> {
                    try {
                        com.visiboard.pc.ui.UserInfoDialog dialog = new com.visiboard.pc.ui.UserInfoDialog(uid, uName);
                        dialog.showAndWait();
                    } catch (Exception ex) {
                        System.err.println("Error opening profile: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                });
            };
            
            userAvatar.setOnMouseClicked(openProfileHandler);
            userAvatar.setCursor(javafx.scene.Cursor.HAND);
            
            userNameLabel.setOnMouseClicked(openProfileHandler);
            userNameLabel.setCursor(javafx.scene.Cursor.HAND);
        }
    }
    
    private void loadCurrentUserAvatar() {
        String picUrl = UserSession.getInstance().getUserProfilePic();
        String name = UserSession.getInstance().getUserName();
        if (picUrl != null && !picUrl.isEmpty()) {
             currentUserAvatar.setVisible(true);
             currentUserAvatar.setManaged(true);
             // Clip circular
             Circle clip = new Circle(16, 16, 16);
             currentUserAvatar.setClip(clip);
             
             com.visiboard.pc.service.ImageCacheService.getInstance()
                .getImage(picUrl)
                .thenAccept(image -> {
                    javafx.application.Platform.runLater(() -> {
                        if (image != null) cropAndSetImage(currentUserAvatar, image);
                    });
                });
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
                            cropAndSetImage(avatar, image);
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
        nameLabel.setStyle("-fx-font-weight: bold; -fx-cursor: hand;");
        
        Label commentLabel = new Label(comment.getContent() != null ? comment.getContent() : "");
        commentLabel.setWrapText(true);
        commentLabel.setMaxWidth(300);
        
        contentBox.getChildren().addAll(nameLabel, commentLabel);
        commentBox.getChildren().addAll(avatar, contentBox);
        
        // Add click handlers for profile dialog
        if (comment.getUser() != null && comment.getUser().getFirebaseUid() != null) {
            String uid = comment.getUser().getFirebaseUid();
            String uName = userName;
            javafx.event.EventHandler<javafx.scene.input.MouseEvent> openProfileHandler = e -> {
                javafx.application.Platform.runLater(() -> {
                     try {
                         com.visiboard.pc.ui.UserInfoDialog dialog = new com.visiboard.pc.ui.UserInfoDialog(uid, uName);
                         dialog.showAndWait();
                     } catch (Exception ex) { ex.printStackTrace(); }
                });
            };
            avatar.setOnMouseClicked(openProfileHandler);
            avatar.setCursor(javafx.scene.Cursor.HAND);
            nameLabel.setOnMouseClicked(openProfileHandler); // Already set cursor hand above
        }
        
        commentsContainer.getChildren().add(commentBox);
    }
    
    private void cropAndSetImage(ImageView imageView, javafx.scene.image.Image image) {
        if (image == null) return;
        
        double width = image.getWidth();
        double height = image.getHeight();
        
        // Calculate crop dimensions for center square
        double size = Math.min(width, height);
        double x = (width - size) / 2.0;
        double y = (height - size) / 2.0;
        
        // Set viewport to show center square
        imageView.setViewport(new javafx.geometry.Rectangle2D(x, y, size, size));
        imageView.setImage(image);
    }
    
    private void setDefaultAvatar(ImageView avatar, String name) {
        String initial = (name != null && !name.isEmpty()) ? name.substring(0, 1).toUpperCase() : "?";
        String url = "https://ui-avatars.com/api/?name=" + initial + "&background=e94560&color=fff&size=64";
        com.visiboard.pc.service.ImageCacheService.getInstance()
            .getImage(url)
            .thenAccept(image -> {
                if (image != null) {
                    javafx.application.Platform.runLater(() -> cropAndSetImage(avatar, image));
                }
            });
    }

    @FXML
    private void handleLike() {
        if (note == null) return;
        System.out.println("[Like] Toggling like for note: " + note.getId());
        apiService.toggleLike(note.getId().toString()).thenAccept(updatedNote -> {
            if (updatedNote != null) {
                System.out.println("[Like] Received updated note with " + updatedNote.getLikesCount() + " likes");
                System.out.println("[Like] Updated likedByUsers: " + updatedNote.getLikedByUsers());
                this.note = updatedNote;
                javafx.application.Platform.runLater(() -> {
                    System.out.println("[Like] Updating UI after like toggle");
                    updateUI();
                });
            } else {
                System.err.println("[Like] Received null updated note");
            }
        }).exceptionally(e -> {
            System.err.println("[Like] Error toggling like: " + e.getMessage());
            e.printStackTrace();
            return null;
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
