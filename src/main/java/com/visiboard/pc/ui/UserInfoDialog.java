package com.visiboard.pc.ui;

import com.visiboard.pc.model.User;
import com.visiboard.pc.service.ApiService;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import com.visiboard.pc.util.UserSession;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserInfoDialog extends Dialog<Void> {

    private final ApiService apiService;

    public UserInfoDialog(String firebaseUid, String fallbackName) {
        this.apiService = new ApiService();

        setTitle("User Profile");
        setHeaderText(null);

        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        dialogPane.setStyle("-fx-background-color: #1a1a2e;");

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPrefWidth(350);
        content.setStyle("-fx-padding: 20;");

        // Loading indicator
        ProgressIndicator progress = new ProgressIndicator();
        content.getChildren().add(progress);
        dialogPane.setContent(content);

        // Fetch user data via new endpoint or just search users (assuming getAllUsers for now or iterate)
        // Since we don't have getUserByFirebaseUid endpoint yet, we might rely on the fact that existing User model
        // has these fields if we fetch them. Actually, simpler to just assume we might have the User object
        // OR we add a method to fetch user by Firebase UID in backend?
        // Wait, the user said "universal window".
        // Let's try to fetch all users and filter (inefficient but works for demo) OR add endpoint.
        // Better: ApiService.getUserByEmail is unique.
        // Let's iterate all users for now as fallback, or use the Notification sender info for initial display.
        
        // Actually, we need to show stats.
        // Let's assume we can find the user.
        
        loaduUserData(firebaseUid, fallbackName, content);
    }
    
    // Fallback: Pass User object directly
    public UserInfoDialog(User user) {
        this.apiService = new ApiService();
        setTitle("User Profile");
        setHeaderText(null);
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        dialogPane.setStyle("-fx-background-color: #1a1a2e;");
        
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPrefWidth(350);
        content.setStyle("-fx-padding: 20;");
        
        displayUser(user, content);
        dialogPane.setContent(content);
    }

    private void loaduUserData(String idInput, String fallbackName, VBox content) {
        System.out.println("[UserInfoDialog] Fetching user: " + idInput);

        java.util.concurrent.CompletableFuture<User> userFuture;
        
        // Check if input looks like a UUID (8-4-4-4-12 hex digits)
        // Simple check: length 36 and contains 4 hyphens
        if (idInput != null && idInput.length() == 36 && idInput.split("-").length == 5) {
             System.out.println("[UserInfoDialog] Detected UUID format, fetching by ID");
             userFuture = apiService.getUserById(idInput);
        } else {
             System.out.println("[UserInfoDialog] Treating as Firebase UID");
             userFuture = apiService.getUserByFirebaseUid(idInput);
        }

        userFuture.thenAccept(user -> {
            System.out.println("[UserInfoDialog] Fetched user: " + (user != null ? user.getName() : "null"));
            if (user != null) {
                System.out.println("Stats: Likes=" + user.getTotalLikesReceived() + ", Followers=" + user.getFollowersCount() + ", ID=" + user.getFirebaseUid());
            }
            Platform.runLater(() -> {
                if (user != null) {
                    displayUser(user, content);
                } else {
                    // Fallback to name only if fetch fails
                    System.out.println("[UserInfoDialog] User object is null, using fallback.");
                    displayFallback(fallbackName, content);
                }
            });
        }).exceptionally(e -> {
             System.err.println("[UserInfoDialog] Error fetching user: " + e.getMessage());
             e.printStackTrace();
             Platform.runLater(() -> displayFallback(fallbackName, content));
             return null;
        });
    }
    
    private void displayFallback(String name, VBox content) {
        content.getChildren().clear();
        
        // 1. Fallback Avatar
        ImageView avatar = new ImageView();
        avatar.setFitWidth(100);
        avatar.setFitHeight(100);
        Circle clip = new Circle(50, 50, 50);
        avatar.setClip(clip);
        
        String initial = (name != null && !name.isEmpty()) ? name.substring(0, 1).toUpperCase() : "?";
        String url = "https://ui-avatars.com/api/?name=" + initial + "&background=e94560&color=fff&size=100";
        Image image = new Image(url, true); // background loading
        avatar.setImage(image);
        
        content.getChildren().add(avatar);

        // 2. Name
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        content.getChildren().add(nameLabel);
        
        // 3. Info
        Label infoLabel = new Label("User details not available");
        infoLabel.setStyle("-fx-text-fill: #808080;");
        content.getChildren().add(infoLabel);
    }

    private void displayUser(User user, VBox content) {
        content.getChildren().clear();

        try {
            // 1. Profile Pic (Method: ImageView with Viewport crop + Clip, same as NoteDetailController)
            ImageView avatarImageView = new ImageView();
            avatarImageView.setFitWidth(100);
            avatarImageView.setFitHeight(100);
            Circle clip = new Circle(50, 50, 50);
            avatarImageView.setClip(clip);

            boolean imageLoaded = false;
            Image image = null;
            
            if (user.getProfilePicUrl() != null && !user.getProfilePicUrl().isEmpty()) {
                try {
                    String base64 = user.getProfilePicUrl();
                    if (base64.startsWith("http")) {
                         image = new Image(base64, true);
                    } else {
                        if (base64.contains(",")) base64 = base64.split(",")[1];
                        base64 = base64.replaceAll("\\s", "");
                        byte[] imageBytes = Base64.getDecoder().decode(base64);
                        image = new Image(new ByteArrayInputStream(imageBytes));
                    }
                    imageLoaded = true;
                } catch (Exception e) {
                    System.err.println("Error loading profile pic: " + e.getMessage());
                }
            }
            
            if (!imageLoaded || image == null) {
                 String initial = (user.getName() != null && !user.getName().isEmpty()) ? user.getName().substring(0, 1).toUpperCase() : "?";
                 String url = "https://ui-avatars.com/api/?name=" + initial + "&background=e94560&color=fff&size=200";
                 image = new Image(url, true);
            }
            
            if (image != null) {
                 // Manual Center Crop logic
                 double width = image.getWidth();
                 double height = image.getHeight();
                 if (width > 0 && height > 0) {
                     double size = Math.min(width, height);
                     double x = (width - size) / 2.0;
                     double y = (height - size) / 2.0;
                     avatarImageView.setViewport(new javafx.geometry.Rectangle2D(x, y, size, size));
                     avatarImageView.setImage(image);
                 }
            }
            
            content.getChildren().add(avatarImageView);

            // 2. Name
            String displayName = (user.getName() != null && !user.getName().isEmpty()) ? user.getName() : "Unknown User";
            System.out.println("[UserInfoDialog] Displaying Name: " + displayName);
            Label nameLabel = new Label(displayName);
            nameLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
            nameLabel.setStyle("-fx-text-fill: white;");
            content.getChildren().add(nameLabel);

            // 3. Stats Grid
            System.out.println("[UserInfoDialog] Displaying Stats");
            HBox statsBox = new HBox(20);
            statsBox.setAlignment(Pos.CENTER);
            
            statsBox.getChildren().add(createStatItem("Likes", String.valueOf(user.getTotalLikesReceived())));
            statsBox.getChildren().add(createStatItem("Followers", String.valueOf(user.getFollowersCount())));
            statsBox.getChildren().add(createStatItem("Following", String.valueOf(user.getFollowingCount())));
            content.getChildren().add(statsBox);

            // 4. Joined Date
            if (user.getCreatedAt() != null) {
                try {
                    String dateStr = user.getCreatedAt(); 
                    if (dateStr.length() > 10) dateStr = dateStr.substring(0, 10);
                    Label joinedLabel = new Label("Joined " + dateStr);
                    joinedLabel.setStyle("-fx-text-fill: #808080; -fx-font-size: 14px;");
                    content.getChildren().add(joinedLabel);
                } catch (Exception e) {}
            }
            
            // 5. Follow Button (if not self)
            String myUid = UserSession.getInstance().getFirebaseUid();
            if (myUid != null && !myUid.equals(user.getFirebaseUid())) {
                 Button followBtn = new Button("Loading...");
                 followBtn.setPrefWidth(120);
                 followBtn.setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                 
                 // Check status
                 if (user.getFirebaseUid() != null) {
                     apiService.isFollowing(user.getFirebaseUid()).thenAccept(isFollowing -> {
                         Platform.runLater(() -> updateFollowButton(followBtn, isFollowing, user.getFirebaseUid()));
                     });
                     content.getChildren().add(followBtn);
                 }
            }
            
            // FORCE RESIZE to make sure content is visible
            Platform.runLater(() -> {
                if (getDialogPane().getScene() != null && getDialogPane().getScene().getWindow() != null) {
                    getDialogPane().getScene().getWindow().sizeToScene();
                }
            });

        } catch (Exception e) {
            System.err.println("[UserInfoDialog] Error in displayUser: " + e.getMessage());
            e.printStackTrace();
            content.getChildren().add(new Label("Error displaying profile"));
        }
    }
    
    private void updateFollowButton(Button btn, boolean isFollowing, String targetUid) {
        if (isFollowing) {
            btn.setText("Unfollow");
            btn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
            btn.setOnAction(e -> {
                btn.setDisable(true);
                apiService.unfollowUser(targetUid).thenAccept(success -> {
                    Platform.runLater(() -> {
                        btn.setDisable(false);
                        if (success) updateFollowButton(btn, false, targetUid);
                    });
                });
            });
        } else {
            btn.setText("Follow");
            btn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
            btn.setOnAction(e -> {
                btn.setDisable(true);
                apiService.followUser(targetUid).thenAccept(success -> {
                    Platform.runLater(() -> {
                        btn.setDisable(false);
                        if (success) updateFollowButton(btn, true, targetUid);
                    });
                });
            });
        }
    }

    private VBox createStatItem(String label, String value) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        
        Label valLabel = new Label(value);
        valLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        valLabel.setStyle("-fx-text-fill: #e94560;"); // Accent color
        
        Label textLabel = new Label(label);
        textLabel.setFont(Font.font("System", 12));
        textLabel.setStyle("-fx-text-fill: #b0b0b0;");
        
        box.getChildren().addAll(valLabel, textLabel);
        return box;
    }
}
