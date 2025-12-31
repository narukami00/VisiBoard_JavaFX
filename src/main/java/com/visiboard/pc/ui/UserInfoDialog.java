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

            // 2. Name & Status Badge
            String displayName = (user.getName() != null && !user.getName().isEmpty()) ? user.getName() : "Unknown User";
            System.out.println("[UserInfoDialog] Displaying Name: " + displayName);
            
            HBox nameContainer = new HBox(10);
            nameContainer.setAlignment(Pos.CENTER);
            
            Label nameLabel = new Label(displayName);
            nameLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
            nameLabel.setStyle("-fx-text-fill: white;");
            
            // Status text
            Label statusLabel = new Label();
             statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
            
            Circle statusBadge = new Circle(8);
            if (user.isBanned()) {
                statusBadge.setFill(javafx.scene.paint.Color.RED);
                 String expiry = user.getBanExpiry() > 0 ? new java.util.Date(user.getBanExpiry()).toString() : "Permanent";
                 statusLabel.setText("Banned until " + expiry);
            } else if (user.isRestricted()) {
                statusBadge.setFill(javafx.scene.paint.Color.ORANGE); // Yellow/Orange
                 String expiry = user.getRestrictionExpiry() > 0 ? new java.util.Date(user.getRestrictionExpiry()).toString() : "Permanent";
                 statusLabel.setText("Restricted until " + expiry);
            } else {
                statusBadge.setFill(javafx.scene.paint.Color.GREEN);
                statusLabel.setText("Active");
            }
            
            nameContainer.getChildren().addAll(nameLabel, statusBadge);
            // Add status text below name
            VBox nameBox = new VBox(5);
            nameBox.setAlignment(Pos.CENTER);
            nameBox.getChildren().addAll(nameContainer, statusLabel);
            content.getChildren().add(nameBox);

            // 3. Stats Grid
            System.out.println("[UserInfoDialog] Displaying Stats");
            HBox statsBox = new HBox(20);
            statsBox.setAlignment(Pos.CENTER);
            
            statsBox.getChildren().add(createStatItem("Likes", String.valueOf(user.getTotalLikesReceived())));
            statsBox.getChildren().add(createStatItem("Followers", String.valueOf(user.getFollowersCount())));
            statsBox.getChildren().add(createStatItem("Following", String.valueOf(user.getFollowingCount())));
            content.getChildren().add(statsBox);

            // 4. Admin Actions
            HBox actionsBox = new HBox(10);
            actionsBox.setAlignment(Pos.CENTER);
            actionsBox.setStyle("-fx-padding: 10 0 10 0;");
            
            Button warnBtn = new Button("Warn User");
            warnBtn.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: black; -fx-font-weight: bold;");
            warnBtn.setOnAction(e -> {
                com.visiboard.pc.services.DatabaseService.warnUser(user.getId());
                warnBtn.setText("Warned");
                warnBtn.setDisable(true);
            });
            
            Button restrictBtn = new Button(user.isRestricted() ? "Unrestrict" : "Restrict");
            restrictBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold;");
            restrictBtn.setOnAction(e -> {
                if (user.isRestricted()) {
                    // Unrestrict
                    com.visiboard.pc.services.DatabaseService.updateUserStatus(user.getId(), "restricted", false, 0);
                    com.visiboard.pc.services.DatabaseService.notifyUser(user.getId(), "Restriction removed.");
                    restrictBtn.setText("Restrict");
                    user.setRestricted(false);
                } else {
                     // Restrict with duration
                     long expiry = showDurationDialog("Restrict");
                     if (expiry != -1) {
                         com.visiboard.pc.services.DatabaseService.updateUserStatus(user.getId(), "restricted", true, expiry);
                         String durationStr = expiry == 0 ? "permanently" : "until " + new java.util.Date(expiry).toString();
                         com.visiboard.pc.services.DatabaseService.notifyUser(user.getId(), "Restricted " + durationStr);
                         restrictBtn.setText("Unrestrict");
                         user.setRestricted(true);
                     }
                }
                updateStatusBadge(user, statusBadge, statusLabel);
            });
            
            Button banBtn = new Button(user.isBanned() ? "Unban" : "Ban");
            banBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
            banBtn.setOnAction(e -> {
                 if (user.isBanned()) {
                     com.visiboard.pc.services.DatabaseService.updateUserStatus(user.getId(), "banned", false, 0);
                     com.visiboard.pc.services.DatabaseService.notifyUser(user.getId(), "Unbanned.");
                     banBtn.setText("Ban");
                     user.setBanned(false);
                 } else {
                     long expiry = showDurationDialog("Ban");
                     if (expiry != -1) {
                         com.visiboard.pc.services.DatabaseService.updateUserStatus(user.getId(), "banned", true, expiry);
                         String durationStr = expiry == 0 ? "permanently" : "until " + new java.util.Date(expiry).toString();
                         com.visiboard.pc.services.DatabaseService.notifyUser(user.getId(), "Banned " + durationStr);
                         banBtn.setText("Unban");
                         user.setBanned(true);
                     }
                 }
                 updateStatusBadge(user, statusBadge, statusLabel);
            });

            actionsBox.getChildren().addAll(warnBtn, restrictBtn, banBtn);
            content.getChildren().add(actionsBox);
            
            getDialogPane().setContent(content);

            
            getDialogPane().setStyle("-fx-background-color: #2c2c44;");




            // 5. User Notes Grid
            Label notesHeader = new Label("User Notes");
            notesHeader.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 10 0 0 0;");
            content.getChildren().add(notesHeader);
            
            ScrollPane gridScroll = new ScrollPane();
            gridScroll.setPrefHeight(300);
            gridScroll.setFitToWidth(true);
            gridScroll.setStyle("-fx-background: #1a1a2e; -fx-border-color: transparent;");
            
            javafx.scene.layout.FlowPane notesGrid = new javafx.scene.layout.FlowPane();
            notesGrid.setHgap(10);
            notesGrid.setVgap(10);
            notesGrid.setPadding(new javafx.geometry.Insets(5));
            notesGrid.setAlignment(Pos.TOP_LEFT);
            notesGrid.setStyle("-fx-background-color: #1a1a2e;");
            
            gridScroll.setContent(notesGrid);
            content.getChildren().add(gridScroll);
            
            // Fetch Notes
            new Thread(() -> {
                java.util.List<com.visiboard.pc.model.Note> notes = com.visiboard.pc.services.DatabaseService.getNotesByUserId(user.getId());
                Platform.runLater(() -> {
                    if (notes.isEmpty()) {
                        notesGrid.getChildren().add(new Label("No notes found for this user."));
                    } else {
                        for (com.visiboard.pc.model.Note note : notes) {
                             notesGrid.getChildren().add(createNoteCard(note));
                        }
                    }
                });
            }).start();

            // 6. Joined Date (moved below)
            Label joinedLabel = new Label(); 
            joinedLabel.setStyle("-fx-text-fill: #808080; -fx-font-size: 14px;");
            if (user.getCreatedAt() > 0) {
                try {
                    String dateStr = new java.util.Date(user.getCreatedAt()).toString();
                    joinedLabel.setText("Joined " + dateStr);
                } catch (Exception e) {
                    joinedLabel.setText("Joined recently");
                }
            } else {
                joinedLabel.setText("Joined recently"); 
            }
            content.getChildren().add(joinedLabel); 
            
            // 7. Follow Button (if not self) - kept as is but renamed section count
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

    private VBox createNoteCard(com.visiboard.pc.model.Note note) {
        VBox card = new VBox(5);
        card.setPrefWidth(100);
        card.setPrefHeight(120); // Slightly shorter
        card.setStyle("-fx-background-color: #252a34; -fx-background-radius: 8; -fx-padding: 0; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 5, 0, 0, 2);");
        card.setAlignment(Pos.TOP_CENTER);
        card.setCursor(javafx.scene.Cursor.HAND);
        
        // Image Container for Clipping
        javafx.scene.layout.StackPane imgContainer = new javafx.scene.layout.StackPane();
        imgContainer.setPrefSize(100, 100);
        imgContainer.setMaxSize(100, 100);
        // Clip to rounded top corners
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(100, 100);
        clip.setArcWidth(8);
        clip.setArcHeight(8);
        imgContainer.setClip(clip);

        ImageView imgView = new ImageView();
        imgView.setFitWidth(100);
        imgView.setFitHeight(100);
        imgView.setPreserveRatio(true);
        
        boolean hasImage = false;
        String imgUrl = note.getImageUrl();
        if (imgUrl != null && !imgUrl.isEmpty()) {
            try {
                Image img;
                if (imgUrl.startsWith("http")) {
                    img = new Image(imgUrl, 150, 150, true, true, true); // Load slightly larger for crispness
                } else {
                     String clean = imgUrl.contains(",") ? imgUrl.split(",")[1] : imgUrl;
                     clean = clean.replaceAll("\\s", "");
                     byte[] b = java.util.Base64.getDecoder().decode(clean);
                     img = new Image(new java.io.ByteArrayInputStream(b));
                }
                
                if (img != null) {
                    imgView.setImage(img);
                    
                    // True Center Crop Logic (Aspect Fill)
                    double imgW = img.getWidth();
                    double imgH = img.getHeight();
                    double wrapperSize = 100.0;
                    
                    if (imgW > 0 && imgH > 0) {
                         // Scale such that the smaller dimension fits the 100px box
                         double scale = Math.max(wrapperSize / imgW, wrapperSize / imgH);
                         double scaledW = imgW * scale;
                         double scaledH = imgH * scale;
                         
                         // We can't resize image easily without expensive ops, 
                         // so we use Viewport on the original image.
                         // Goal: define a square viewport on the original image that covers the center.
                         double cropDim = Math.min(imgW, imgH);
                         double x = (imgW - cropDim) / 2;
                         double y = (imgH - cropDim) / 2;
                         
                         imgView.setViewport(new javafx.geometry.Rectangle2D(x, y, cropDim, cropDim));
                         imgView.setFitWidth(100);
                         imgView.setFitHeight(100);
                         imgView.setPreserveRatio(false); // Since viewport is square, this is safe
                    }
                    
                    hasImage = true;
                }
            } catch(Exception e) {
                // Ignore
            }
        }
        
        if (!hasImage) {
            // Text visual
            Label textPreview = new Label(note.getContent());
            textPreview.setWrapText(true);
            textPreview.setStyle("-fx-text-fill: #aaa; -fx-font-size: 9px; -fx-padding: 5;");
            textPreview.setPrefSize(100, 100);
            textPreview.setAlignment(Pos.TOP_LEFT);
            imgContainer.getChildren().add(textPreview);
        } else {
            imgContainer.getChildren().add(imgView);
        }
        
        card.getChildren().add(imgContainer);

        // Footer with Likes and Date
        HBox footer = new HBox(5);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new javafx.geometry.Insets(2, 5, 2, 5));
        
        Label likesLabel = new Label("♥ " + note.getLikesCount());
        likesLabel.setStyle("-fx-text-fill: #e94560; -fx-font-size: 10px; -fx-font-weight: bold;");
        
        footer.getChildren().addAll(likesLabel);
        card.getChildren().add(footer);
        
        // Click Action -> Open NoteDetail
        card.setOnMouseClicked(e -> openNoteDetails(note));
        
        return card;
    }

    private void openNoteDetails(com.visiboard.pc.model.Note note) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/visiboard/pc/view/note_detail_view.fxml"));
            javafx.scene.Parent root = loader.load();
            
            com.visiboard.pc.controller.NoteDetailController controller = loader.getController();
            // Pass the note ID and API service. 
            // Note: DB service is static, but controller usually takes ApiService. 
            // We can re-use existing logic using just ID.
            controller.setNote(note.getId(), this.apiService);
            
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Note Details");
            stage.setScene(scene);
            
            // Re-use map logic style
             stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
             
             controller.setOnNoteDeleted(() -> {
                 // Refresh list if deleted
                 // card.getChildren().clear(); // REMOVED: card is not accessible here.
                 // For now just close the detail window. Ideally we'd refresh the parent list.
                 stage.close();
                 // TODO: Trigger a refresh of the user dialog notes list if possible
             });
             
            stage.show();
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
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

    private void updateStatusBadge(com.visiboard.pc.model.User user, Circle badge, Label label) {
         if (user.isBanned()) {
             badge.setFill(javafx.scene.paint.Color.RED);
             String expiry = user.getBanExpiry() > 0 ? new java.util.Date(user.getBanExpiry()).toString() : "Permanent";
             label.setText("Banned until " + expiry);
         } else if (user.isRestricted()) {
             badge.setFill(javafx.scene.paint.Color.ORANGE);
             String expiry = user.getRestrictionExpiry() > 0 ? new java.util.Date(user.getRestrictionExpiry()).toString() : "Permanent";
             label.setText("Restricted until " + expiry);
         } else {
             badge.setFill(javafx.scene.paint.Color.GREEN);
             label.setText("Active");
         }
    }

    private long showDurationDialog(String action) {
        java.util.List<String> options = java.util.Arrays.asList("24 Hours", "3 Days", "1 Week", "1 Month", "Permanent");
        javafx.scene.control.ChoiceDialog<String> dialog = new javafx.scene.control.ChoiceDialog<>("24 Hours", options);
        dialog.setTitle(action + " Duration");
        dialog.setHeaderText("Select " + action + " Duration");
        dialog.setContentText("Duration:");
        
        java.util.Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String duration = result.get();
            long now = System.currentTimeMillis();
            switch (duration) {
                case "24 Hours": return now + (24 * 3600 * 1000L);
                case "3 Days": return now + (3 * 24 * 3600 * 1000L);
                case "1 Week": return now + (7 * 24 * 3600 * 1000L);
                case "1 Month": return now + (30 * 24 * 3600 * 1000L);
                case "Permanent": return 0;
                default: return 0;
            }
        }
        return -1; // Cancelled
    }
}
